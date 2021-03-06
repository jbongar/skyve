package org.skyve.impl.web.faces.actions;

import java.util.List;
import java.util.Map;

import org.skyve.domain.Bean;
import org.skyve.impl.util.UtilImpl;
import org.skyve.impl.web.faces.FacesAction;
import org.skyve.metadata.module.query.DocumentQueryDefinition;
import org.skyve.persistence.DocumentFilter;
import org.skyve.persistence.DocumentQuery;
import org.skyve.util.Util;

public class GetBeansAction extends FacesAction<List<Bean>> {
	private String bizModule;
	private String queryName;
	private Map<String, Object> parameters;
	public GetBeansAction(String bizModule, String queryName, Map<String, Object> parameters) {
		this.bizModule = bizModule;
		this.queryName = queryName;
		this.parameters = parameters;
	}
	
	@Override
	public List<Bean> callback() throws Exception {
		if (UtilImpl.FACES_TRACE) Util.LOGGER.info("GetBeansAction - bizModule=" + bizModule + " : queryName=" + queryName);

		DocumentQueryDefinition query = ActionUtil.getDocumentQuery(bizModule, queryName);
		DocumentQuery documentQuery = query.constructDocumentQuery(null, null).setFirstResult(0).setMaxResults(250);
		
		if (parameters != null) {
			StringBuilder substring = new StringBuilder(32);
			DocumentFilter documentFilter = documentQuery.getFilter();
			for (String parameterName : parameters.keySet()) {
				substring.setLength(0);
				substring.append('%').append(parameters.get(parameterName)).append('%');
				documentFilter.addLike(parameterName, substring.toString());
			}
		}

		return documentQuery.projectedResults();
	}
}
