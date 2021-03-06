package org.skyve.impl.job;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.skyve.CORE;
import org.skyve.EXT;
import org.skyve.content.ContentManager;
import org.skyve.content.SearchResult;
import org.skyve.domain.Bean;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.repository.Repository;
import org.skyve.persistence.Persistence;
import org.skyve.persistence.SQL;
import org.skyve.util.Util;

/**
 * This job removes orphaned uploads and any textually indexed data left from delete/truncate SQL statements issued.
 * 
 * @author sandsm01
 */
public class ContentGarbageCollectionJob implements Job {
	private Set<String> orphanedContentIds = new TreeSet<>();
	
	@Override
	public void execute(JobExecutionContext context)
	throws JobExecutionException {
		try {
			Repository r = CORE.getRepository();
			Persistence p = CORE.getPersistence();
			try {
				try (ContentManager cm = EXT.newContentManager()) {
					for (SearchResult result : cm.all()) {
						try { // don't stop trying to detect removed content
							if (UtilImpl.CONTENT_TRACE) UtilImpl.LOGGER.finest("ContentGarbageCollectionJob: FOUND customer=" + result.getCustomerName() + 
																				" : module=" + result.getModuleName() + 
																				" : document=" + result.getDocumentName() + 
																				" : bizId=" + result.getBizId() + 
																				" : attribute=" + result.getAttributeName() + 
																				" : lastModified=" + result.getLastModified());
							Date lastModified = result.getLastModified();
							// only process this if its at least a day old.
							// Besides cutting out busy work on a data set in flux, it'll make sure that anyones freshly uploaded
							// content that hasn't been saved (not pointed to yet in the database) won't be removed.
							if ((lastModified == null) || ((System.currentTimeMillis() - lastModified.getTime()) > 86400000)) {
								Customer customer = r.getCustomer(result.getCustomerName());
								Module module = customer.getModule(result.getModuleName());
								Document document = module.getDocument(customer, result.getDocumentName());
								
								SQL query = null;
								StringBuilder sql = new StringBuilder(128);
								sql.append("select 1 from ").append(document.getPersistent().getPersistentIdentifier());
								sql.append(" where ").append(Bean.DOCUMENT_ID).append(" = :").append(Bean.DOCUMENT_ID);
								
								// check if we have a record
								String attributeName = result.getAttributeName();
								if (attributeName != null) { // attachment
									sql.append(" and ").append(attributeName).append(" = :").append(attributeName);
		
									query = p.newSQL(sql.toString());
									query.putParameter(Bean.DOCUMENT_ID, result.getBizId(), false);
									query.putParameter(attributeName, result.getContentId(), false);
								}
								else { // bean
									query = p.newSQL(sql.toString());
									query.putParameter(Bean.DOCUMENT_ID, result.getBizId(), false);
								}
								
								if (UtilImpl.CONTENT_TRACE) UtilImpl.LOGGER.finest("ContentGarbageCollectionJob: TEST REMOVAL with " + sql.toString());
								if (query.scalarResults(Integer.class).isEmpty()) {
									orphanedContentIds.add(result.getContentId());
								}
							}
						}
						catch (Exception e) {
							Util.LOGGER.warning("ContentGarbageCollectionJob retrieve problem..." + e.getLocalizedMessage());
							if (UtilImpl.CONTENT_TRACE) Util.LOGGER.log(Level.WARNING, "ContentGarbageCollectionJob.execute() problem...", e);
						}
					}
					
					for (String contentId : orphanedContentIds) {
						try { // don't stop trying to remove content
							if (UtilImpl.CONTENT_TRACE) UtilImpl.LOGGER.info("ContentGarbageCollectionJob: Remove content with ID " + contentId);
							cm.remove(contentId);
						}
						catch (Exception e) {
							Util.LOGGER.warning("ContentGarbageCollectionJob remove problem..." + e.getLocalizedMessage());
							if (UtilImpl.CONTENT_TRACE) Util.LOGGER.log(Level.WARNING, "ContentGarbageCollectionJob.execute() problem...", e);
						}
					}
				}
			}
			finally {
				p.commit(true);
			}
			if (UtilImpl.CONTENT_TRACE) Util.LOGGER.info("Successfully performed CMS garbage collection");
		}
		catch (Exception e) {
			throw new JobExecutionException("Error encountered whilst performing CMS garbage collection", e);
		}
	}
}