package org.skyve.impl.generate;

import java.util.Map.Entry;

import org.skyve.impl.metadata.repository.AbstractRepository;
import org.skyve.impl.metadata.repository.LocalDesignRepository;
import org.skyve.impl.metadata.repository.router.UxUiMetadata;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.view.View;
import org.skyve.metadata.view.View.ViewType;

public abstract class DomainGenerator {
	protected static final String DECIMAL2 = "Decimal2";
	protected static final String DECIMAL5 = "Decimal5";
	protected static final String DECIMAL10 = "Decimal10";
	protected static final String DATE_ONLY = "DateOnly";
	protected static final String DATE_TIME = "DateTime";
	protected static final String TIME_ONLY = "TimeOnly";
	protected static final String TIMESTAMP = "Timestamp";
	protected static final String GEOMETRY = "Geometry";

	protected static String SRC_PATH;
	protected static String TEST_PATH;
	protected static String GENERATED_TEST_PATH;
	protected static String[] EXCLUDED_MODULES;

	// Cascade type 'merge' makes many-many relationships within the association
	// target object update (without the collection being dirty)
	// and thus causes optimistic lock exceptions when the bizLock
	// is up-revved from the update statement.
	// Case in point is Staff --many-to-one--> User --many-to-many--> Groups,
	// all groups are up-revved, even though the collection is not dirty,
	// causing optimistic lock when Staff are saved.
	// So if lots of Staff use the same user, we're screwed.
	protected static boolean ALLOW_CASCADE_MERGE = true;
	
	protected static boolean DATA_STORE_INDEX_NAMES_IN_GLOBAL_NAMESPACE = false;
	protected static int DATA_STORE_IDENTIFIER_CHARACTER_LIMIT = 0;
	protected static boolean DATA_STORE_INDEX_FOREIGN_KEYS_BY_DEFAULT = true;

	public static final void validate(String customerName) throws Exception {
		AbstractRepository repository = AbstractRepository.get();

		System.out.println("Get customer " + customerName);
		Customer customer = repository.getCustomer(customerName);
		System.out.println("Validate customer " + customerName);
		repository.validateCustomer(customer);
		for (Module module : customer.getModules()) {
			System.out.println("Validate module " + module.getName());
			repository.validateModule(customer, module);
			for (Entry<String, DocumentRef> entry : module.getDocumentRefs().entrySet()) {
				String documentName = entry.getKey();
				System.out.println("Get document " + documentName);
				Document document = module.getDocument(customer, documentName);
				System.out.println("Validate document " + documentName);
				repository.validateDocument(customer, document);
				for (UxUiMetadata uxui : repository.getRouter().getUxUis()) {
					String uxuiName = uxui.getName();
					System.out.println("Get edit view for document " + documentName + " and uxui " + uxuiName);
					View view = repository.getView(uxuiName, customer, document, ViewType.edit);
					System.out.println("Validate edit view for document " + documentName + " and uxui " + uxuiName);
					repository.validateView(customer, document, view, uxuiName);
					view = repository.getView(uxuiName, customer, document, ViewType.create);
					if (view != null) {
						System.out.println("Validate create view for document " + documentName + " and uxui " + uxuiName);
						repository.validateView(customer, document, view, uxuiName);
					}
				}
			}
		}
	}

	public abstract void generate() throws Exception;

	/**
	 * Usage :-
	 * <dl>
	 * <dt>sourcePath</dt>
	 * <dd>path to source files where modules are located</dd>
	 * <dt>testPath</dt>
	 * <dd>path to test files</dd>
	 * <dt>generatedTestPath</dt>
	 * <dd>path to place generated tests</dd>
	 * <dt>allowCascadeMerge</dt>
	 * <dd>optional, true or false</dd>
	 * <dt>debug</dt>
	 * <dd>optional, true or false to enable debug mode</dd>
	 * <dt>data store index foreign keys by default</dt>
	 * <dd>optional, true or false to index foreign keys by default in the data store</dd>
	 * <dt>data store index names in global namespace</dt>
	 * <dd>optional, true or false if data store index names are part of the global namespace</dd>
	 * <dt>data store identifier character limit</dt>
	 * <dd>optional, size of allowed data store identifiers</dd>
	 * <dt>excludedModules</dt>
	 * <dd>optional, comma separated list of modules not to generate unit tests for</dd>
	 * </dl>
	 * 
	 * E.g. src/skyve src/test src/generatedTest,
	 * src/skyve src/test src/generatedTest true,
	 * src/skyve src/test src/generatedTest false false test,whosin
	 * src/skyve src/test src/generatedTest false true whosin
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length == 0 || args.length < 3) {
			System.err.println("You must have at least the src path, test path and generated test path as arguments"
					+ " - usually \"src/skyve/ src/test/ src/generatedTest/\"");
			System.exit(1);
		}

		UtilImpl.COMMAND_TRACE = false;
		UtilImpl.CONTENT_TRACE = false;
		UtilImpl.HTTP_TRACE = false;
		UtilImpl.QUERY_TRACE = false;
		UtilImpl.SECURITY_TRACE = false;
		UtilImpl.BIZLET_TRACE = false;
		UtilImpl.SQL_TRACE = false;
		UtilImpl.XML_TRACE = false;

		SRC_PATH = args[0];
		TEST_PATH = args[1];// "src/test/";
		GENERATED_TEST_PATH = args[2];// "src/generatedTest/";

		if (args.length >= 4) {
			if ("true".equalsIgnoreCase(args[3]) || "false".equalsIgnoreCase(args[3])) {
				ALLOW_CASCADE_MERGE = Boolean.parseBoolean(args[3]);
			} else {
				System.err.println("The fourth argument ALLOW_CASCADE_MERGE should be true or false");
				System.exit(1);
			}
		}

		if (args.length >= 5) { // allow for debug mode if there are 5 arguments
			if ("true".equalsIgnoreCase(args[4]) || "false".equalsIgnoreCase(args[4])) {
				if (Boolean.parseBoolean(args[4])) {
					UtilImpl.COMMAND_TRACE = true;
					UtilImpl.CONTENT_TRACE = true;
					UtilImpl.HTTP_TRACE = true;
					UtilImpl.QUERY_TRACE = true;
					UtilImpl.SECURITY_TRACE = true;
					UtilImpl.BIZLET_TRACE = true;
					UtilImpl.SQL_TRACE = true;
					UtilImpl.XML_TRACE = true;
				}
			} else {
				System.err.println("The fifth argument DEBUG should be true or false");
				System.exit(1);
			}
		}

		if (args.length >= 6) {
			if ("true".equalsIgnoreCase(args[5]) || "false".equalsIgnoreCase(args[5])) {
				DATA_STORE_INDEX_FOREIGN_KEYS_BY_DEFAULT = Boolean.parseBoolean(args[5]);
			}
			else {
				System.err.println("The sixth argument DATA_STORE_INDEX_FOREIGN_KEYS_BY_DEFAULT should be true or false");
				System.exit(1);
			}
		}
		if (args.length >= 7) {
			if ("true".equalsIgnoreCase(args[6]) || "false".equalsIgnoreCase(args[6])) {
				DATA_STORE_INDEX_NAMES_IN_GLOBAL_NAMESPACE = Boolean.parseBoolean(args[6]);
			}
			else {
				System.err.println("The seventh argument DATA_STORE_INDEX_NAMES_IN_GLOBAL_NAMESPACE should be true or false");
				System.exit(1);
			}
		}
		if (args.length >= 8) {
			try {
				DATA_STORE_IDENTIFIER_CHARACTER_LIMIT = Integer.parseInt(args[7]);
			}
			catch (NumberFormatException e) {
				System.err.println("The eighth argument DATA_STORE_IDENTIFIER_CHARACTER_LIMIT should be an integer (0 for unlimited)");
				System.exit(1);
			}
		}
		
		
		if (args.length == 9) {
			if ((args[8] != null) && (! args[8].isEmpty())) {
				EXCLUDED_MODULES = args[8].toLowerCase().split(",");
			}
		}

		System.out.println("SRC_PATH=" + SRC_PATH);
		System.out.println("TEST_PATH=" + TEST_PATH);
		System.out.println("GENERATED_TEST_PATH=" + GENERATED_TEST_PATH);
		System.out.println("ALLOW_CASCADE_MERGE=" + ALLOW_CASCADE_MERGE);
		System.out.println("DEBUG=" + (args.length >= 5 ? args[4] : "false"));
		System.out.println("DATA_STORE_INDEX_FOREIGN_KEYS_BY_DEFAULT=" + (args.length >= 6 ? args[5] : "true"));
		System.out.println("DATA_STORE_INDEX_NAMES_IN_GLOBAL_NAMESPACE=" + (args.length >= 7 ? args[6] : "false"));
		System.out.println("DATA_STORE_IDENTIFIER_CHARACTER_LIMIT=" + (args.length >= 8 ? args[7] : "0"));
		System.out.println("EXCLUDED_MODULES=" + (args.length == 9 ? args[8] : ""));

		DomainGenerator foo = UtilImpl.USING_JPA ? new JPADomainGenerator() : new OverridableDomainGenerator();
		AbstractRepository repository = new LocalDesignRepository();
		AbstractRepository.set(repository);

		// generate for all customers
		for (String customerName : repository.getAllCustomerNames()) {
			validate(customerName);
		}
		foo.generate();
	}
}
