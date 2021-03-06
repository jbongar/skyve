package org.skyve.metadata.view.model.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.domain.MapBean;
import org.skyve.domain.PersistentBean;
import org.skyve.impl.metadata.model.document.CollectionImpl.OrderingImpl;
import org.skyve.impl.web.SortParameter;
import org.skyve.metadata.SortDirection;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Association;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.query.QueryColumn;
import org.skyve.persistence.AutoClosingIterable;
import org.skyve.persistence.DocumentQuery.AggregateFunction;
import org.skyve.util.Binder;
import org.skyve.util.Binder.TargetMetaData;

public abstract class InMemoryListModel<T extends Bean> extends ListModel<T> {
	private static final long serialVersionUID = -4488883647065013017L;

	private Customer customer;
	private Module module;
	private Document drivingDocument;
	private List<Bean> rows;
	
	/**
	 * 
	 * @param module
	 * @param document
	 * @throws Exception
	 */
	public void setDrivingDocument(Module module, Document drivingDocument)
	throws Exception {
		customer = CORE.getUser().getCustomer();
		this.module = module;
		this.drivingDocument = drivingDocument;
		
		projections.add(Bean.DOCUMENT_ID);
		projections.add(PersistentBean.LOCK_NAME);
		projections.add(PersistentBean.TAGGED_NAME);
		projections.add(PersistentBean.FLAG_COMMENT_NAME);
		projections.add(Bean.BIZ_KEY);

		for (QueryColumn column : getColumns()) {
			if (column.isProjected()) {
				String binding = column.getBinding();
				if (binding != null) {
					// if this binding is to an association, 
					// add the bizId as the column value and bizKey as the column displayValue
					TargetMetaData target = Binder.getMetaDataForBinding(customer,
																			module,
																			drivingDocument,
																			binding);
					if (target.getAttribute() instanceof Association) {
						StringBuilder sb = new StringBuilder(64);
						sb.append(binding).append('.').append(Bean.BIZ_KEY);
						projections.add(sb.toString());
					}
				}
				projections.add((binding != null) ? binding : column.getName());
			}
		}
	}
	
	/**
	 * The model is destructive to the collection of rows so ensure you return a copy if required.
	 * 
	 * @param rows
	 */
	public abstract List<Bean> getRows() throws Exception;
	
	@Override
	public Document getDrivingDocument() {
		return drivingDocument;
	}

	private Set<String> projections = new TreeSet<>();
	
	@Override
	public Set<String> getProjections() {
		return projections;
	}

	private InMemoryFilter filter;
	
	@Override
	public final Filter getFilter() throws Exception {
		if (filter == null) {
			filter = (InMemoryFilter) newFilter();
		}
		return filter;
	}

	@Override
	public Filter newFilter() throws Exception {
		return new InMemoryFilter();
	}

	private void filterAndSort() {
		if (filter != null) {
			filter.filter(rows);
		}
		
		SortParameter[] sorts = getSortParameters();
		if (sorts != null) {
			OrderingImpl[] order = new OrderingImpl[sorts.length];
			int i = 0;
			for (SortParameter sort : sorts) {
				String by = sort.getBy();
				SortDirection direction = sort.getDirection();
				order[i++] = new OrderingImpl(by, direction);
			}
			Binder.sortCollectionByOrdering(rows, order);
		}
	}
	
	private Bean summarize() throws Exception {
		Map<String, Object> summaryData = new TreeMap<>();

		AggregateFunction summary = getSummary();
		// This needs to be the ID to satisfy the client data source definitions
		summaryData.put(Bean.DOCUMENT_ID, Long.valueOf(rows.size()));
		summaryData.put(PersistentBean.FLAG_COMMENT_NAME, "");

		if (AggregateFunction.Count.equals(summary)) {
			for (Bean row : rows) {
				for (QueryColumn column : getColumns()) {
					String binding = column.getBinding();
					Object value = Binder.get(row, binding);
					if (value != null) {
						Long count = (Long) summaryData.get(binding);
						count = (count == null) ? Long.valueOf(1) : Long.valueOf(count.longValue() + 1);
						summaryData.put(binding, count);
					}
				}
			}
		}
		else if (AggregateFunction.Min.equals(summary)) {
			minOrMax(summaryData, false);
		}
		else if (AggregateFunction.Max.equals(summary)) {
			minOrMax(summaryData, true);
		}
		else if (AggregateFunction.Sum.equals(summary)) {
			sum(summaryData);
		}
		else if (AggregateFunction.Avg.equals(summary)) {
			sum(summaryData);

			// Now compute the average
			for (QueryColumn column : getColumns()) {
				String binding = column.getBinding();
				Number sum = (Number) summaryData.get(binding);
				if (sum != null) {
					// round to 5dp
					summaryData.put(binding, Double.valueOf(Math.round(sum.doubleValue() / rows.size() * 100000d) / 100000d));
				}
			}
		}
		
		return new MapBean(module.getName(), drivingDocument.getName(), summaryData);
	}
	
	private void minOrMax(Map<String, Object> summaryData, boolean max) throws Exception {
		for (Bean row : rows) {
			for (QueryColumn column : getColumns()) {
				String binding = column.getBinding();
				@SuppressWarnings("unchecked")
				Comparable<Object> value = (Comparable<Object>) Binder.get(row, binding);
				if (value != null) {
					@SuppressWarnings("unchecked")
					Comparable<Object> minOrMax = (Comparable<Object>) summaryData.get(binding);
					if (minOrMax == null) {
						summaryData.put(binding, value);
					}
					else if (max && (value.compareTo(minOrMax) > 0)) {
						summaryData.put(binding, value);
					}
					else if ((! max) && (value.compareTo(minOrMax) < 0)) {
						summaryData.put(binding, value);
					}
				}
			}
		}
	}
	
	private void sum(Map<String, Object> summaryData) throws Exception {
		for (Bean row : rows) {
			for (QueryColumn column : getColumns()) {
				String binding = column.getBinding();
				Object value = Binder.get(row, binding);
				if (value instanceof Number) {
					double number = ((Number) value).doubleValue();
					Number sum = (Number) summaryData.get(binding);
					sum = (sum == null) ? Double.valueOf(number) : Double.valueOf(sum.doubleValue() + number);
					summaryData.put(binding, sum);
				}
			}
		}
		// Round to 5dp
		for (QueryColumn column : getColumns()) {
			String binding = column.getBinding();
			Object value = Binder.get(summaryData, binding);
			if (value instanceof Number) {
				summaryData.put(binding, Double.valueOf(Math.round(((Number) value).doubleValue() * 100000d) / 100000d));
			}
		}
	}
	
	@Override
	public Page fetch() throws Exception {
		rows = getRows();
		if (rows == null) {
			rows = new ArrayList<>(0);
		}
		
		filterAndSort();
		
		int startRow = getStartRow();
		int endRow = getEndRow();
		
		Page result = new Page();
		int totalRows = rows.size();
		result.setTotalRows(totalRows);
		result.setSummary(summarize());

		// If something requests a start row > what we have in the set
		// (maybe a criteria has constrained the set such that a page we were at doesn't exist any more)
		// then just send back an empty result set.
		if (startRow < totalRows) {
			// NB This next bit gets destructive on the list
			rows.retainAll(rows.subList(startRow, Math.min(endRow + 1, totalRows)));
			result.setRows(rows);
		}
		else {
			result.setRows(new ArrayList<Bean>(0));
		}
		
		return result;
	}

	@Override
	public AutoClosingIterable<Bean> iterate() throws Exception {
		rows = getRows();
		if (rows == null) {
			rows = new ArrayList<>(0);
		}

		filterAndSort();
		
		return new AutoClosingIterable<Bean>() {
			@Override
			@SuppressWarnings("synthetic-access")
			public Iterator<Bean> iterator() {
				return rows.iterator();
			}

			@Override
			public void close() throws Exception {
				// nothing to close here
			}
		};
	}
}
