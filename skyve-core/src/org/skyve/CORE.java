package org.skyve;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.SortedMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.skyve.impl.metadata.model.document.CollectionImpl;
import org.skyve.impl.persistence.AbstractPersistence;
import org.skyve.impl.util.ThreadSafeFactory;
import org.skyve.metadata.SortDirection;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Collection.Ordering;
import org.skyve.metadata.repository.Repository;
import org.skyve.metadata.user.User;
import org.skyve.persistence.Persistence;

/**
 * The central factory for creating all objects required in the skyve core API.
 * See {@link org.skyve.EXT} for creating objects implemented in skyve ext.
 */
@ApplicationScoped
public class CORE {
	/**
	 * Disallow instantiation
	 */
	private CORE() {
		// no-op
	}
	
	/**
	 * Get a persistence object associated with the current thread of execution.
     * Note that all persistence related objects can get created through a 
     * {@link org.skyve.persistence.Persistence}.
	 * 
	 * @return The persistence object.
	 */
	@Produces
	public static Persistence getPersistence() {
		return AbstractPersistence.get();
	}
	
	/**
	 * Create a new Ordering object used in {@link org.skyve.util.Binder}
	 * and in {@link org.skyve.persistence.Query}s.
	 * Note that all persistence related objects can get created through a 
	 * {@link org.skyve.persistence.Persistence}.
	 * 
	 * @return The new ordering specification.
	 */
	public static Ordering newOrdering(String by, SortDirection sort) {
		return new CollectionImpl.OrderingImpl(by, sort);
	}
	
	/**
	 * Get the current authenticated user for this thread of execution.
	 * 
	 * @return The current user.
	 */
	@Produces
	public static User getUser() {
		return AbstractPersistence.get().getUser();
	}

	/**
	 * Get the current customer for this thread of execution.
	 * 
	 * @return The current customer.
	 */
	@Produces
	public static Customer getCustomer() {
		return AbstractPersistence.get().getUser().getCustomer();
	}

	/**
	 * A place (thread-local), where state can be stashed for the duration of the conversation.
	 * Bear in mind that this map is serialised and cached in the conversation so manage its size aggressively.
	 */
	@Produces
	public static SortedMap<String, Object> getStash() {
		return AbstractPersistence.get().getStash();
	}
	
	/**
	 * Get the repository for this thread of execution.
	 * This is used to access all skyve's metadata programmatically.
	 * Most metadata is available from {@link org.skyve.metadata.customer.Customer}
	 * through <code>CORE.getUser().getCustomer()<code> or <code>persistence.getUser().getCustomer()</code>.
	 * 
	 * @return The repository.
	 */
	@Produces
	public static Repository getRepository() {
		return org.skyve.impl.metadata.repository.AbstractRepository.get();
	}
	
	/**
	 * Get a date format for the current thread of execution.
	 * Since format objects are not thread-safe, this convenience method exists to return one for each thread.
	 * 
	 * @return A date format.
	 */
	public static SimpleDateFormat getDateFormat(String formatString) {
		return ThreadSafeFactory.getDateFormat(formatString);
	}
	
    /**
     * Get a decimal format for the current thread of execution.
     * Since format objects are not thread-safe, this convenience method exists to return one for each thread.
     * 
     * @return A decimal format.
     */
	public static DecimalFormat getDecimalFormat(String formatString) {
		return ThreadSafeFactory.getDecimalFormat(formatString);
	}
}
