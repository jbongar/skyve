package modules.whosinIntegrate.domain;

import com.vividsolutions.jts.geom.Geometry;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.skyve.CORE;
import org.skyve.impl.domain.AbstractPersistentBean;
import org.skyve.impl.domain.types.jaxb.GeometryMapper;

/**
 * Office
 * <br/>
 * An official location where staff operate.
 * 
 * @stereotype "persistent"
 */
@XmlType
@XmlRootElement
public class Office extends AbstractPersistentBean {
	/**
	 * For Serialization
	 * @hidden
	 */
	private static final long serialVersionUID = 1L;

	/** @hidden */
	public static final String MODULE_NAME = "whosinIntegrate";
	/** @hidden */
	public static final String DOCUMENT_NAME = "Office";

	/** @hidden */
	public static final String levelUnitPropertyName = "levelUnit";
	/** @hidden */
	public static final String buildingNamePropertyName = "buildingName";
	/** @hidden */
	public static final String streetAddressPropertyName = "streetAddress";
	/** @hidden */
	public static final String suburbPropertyName = "suburb";
	/** @hidden */
	public static final String postCodePropertyName = "postCode";
	/** @hidden */
	public static final String phonePropertyName = "phone";
	/** @hidden */
	public static final String boundaryPropertyName = "boundary";

	/**
	 * Level/Unit
	 **/
	private String levelUnit;
	/**
	 * Building Name
	 **/
	private String buildingName;
	/**
	 * Street Address
	 **/
	private String streetAddress;
	/**
	 * Suburb
	 **/
	private String suburb;
	/**
	 * Post Code
	 **/
	private String postCode;
	/**
	 * Phone
	 **/
	private String phone;
	/**
	 * Boundary
	 * <br/>
	 * The boundary around the office.
	 **/
	private Geometry boundary;

	@Override
	@XmlTransient
	public String getBizModule() {
		return Office.MODULE_NAME;
	}

	@Override
	@XmlTransient
	public String getBizDocument() {
		return Office.DOCUMENT_NAME;
	}

	public static Office newInstance() throws Exception {
		return CORE.getUser().getCustomer().getModule(MODULE_NAME).getDocument(CORE.getUser().getCustomer(), DOCUMENT_NAME).newInstance(CORE.getUser());
	}

	@Override
	@XmlTransient
	public String getBizKey() {
return modules.whosinIntegrate.Office.OfficeBizlet.bizKey(this);
	}

	@Override
	public boolean equals(Object o) {
		return ((o instanceof Office) && 
					this.getBizId().equals(((Office) o).getBizId()));
	}

	/**
	 * {@link #levelUnit} accessor.
	 * @return	The value.
	 **/
	public String getLevelUnit() {
		return levelUnit;
	}

	/**
	 * {@link #levelUnit} mutator.
	 * @param levelUnit	The new value.
	 **/
	@XmlElement
	public void setLevelUnit(String levelUnit) {
		preset(levelUnitPropertyName, levelUnit);
		this.levelUnit = levelUnit;
	}

	/**
	 * {@link #buildingName} accessor.
	 * @return	The value.
	 **/
	public String getBuildingName() {
		return buildingName;
	}

	/**
	 * {@link #buildingName} mutator.
	 * @param buildingName	The new value.
	 **/
	@XmlElement
	public void setBuildingName(String buildingName) {
		preset(buildingNamePropertyName, buildingName);
		this.buildingName = buildingName;
	}

	/**
	 * {@link #streetAddress} accessor.
	 * @return	The value.
	 **/
	public String getStreetAddress() {
		return streetAddress;
	}

	/**
	 * {@link #streetAddress} mutator.
	 * @param streetAddress	The new value.
	 **/
	@XmlElement
	public void setStreetAddress(String streetAddress) {
		preset(streetAddressPropertyName, streetAddress);
		this.streetAddress = streetAddress;
	}

	/**
	 * {@link #suburb} accessor.
	 * @return	The value.
	 **/
	public String getSuburb() {
		return suburb;
	}

	/**
	 * {@link #suburb} mutator.
	 * @param suburb	The new value.
	 **/
	@XmlElement
	public void setSuburb(String suburb) {
		preset(suburbPropertyName, suburb);
		this.suburb = suburb;
	}

	/**
	 * {@link #postCode} accessor.
	 * @return	The value.
	 **/
	public String getPostCode() {
		return postCode;
	}

	/**
	 * {@link #postCode} mutator.
	 * @param postCode	The new value.
	 **/
	@XmlElement
	public void setPostCode(String postCode) {
		preset(postCodePropertyName, postCode);
		this.postCode = postCode;
	}

	/**
	 * {@link #phone} accessor.
	 * @return	The value.
	 **/
	public String getPhone() {
		return phone;
	}

	/**
	 * {@link #phone} mutator.
	 * @param phone	The new value.
	 **/
	@XmlElement
	public void setPhone(String phone) {
		preset(phonePropertyName, phone);
		this.phone = phone;
	}

	/**
	 * {@link #boundary} accessor.
	 * @return	The value.
	 **/
	public Geometry getBoundary() {
		return boundary;
	}

	/**
	 * {@link #boundary} mutator.
	 * @param boundary	The new value.
	 **/
	@XmlJavaTypeAdapter(GeometryMapper.class)
	@XmlElement
	public void setBoundary(Geometry boundary) {
		preset(boundaryPropertyName, boundary);
		this.boundary = boundary;
	}

	/**
	 * Created
	 * @return	The condition

	 */
	@XmlTransient
	@Override
	public boolean isCreated() {
		return (isPersisted());
	}

	/**	 * {@link #isCreated} negation.

	 * @return	The negated condition

	 */
	@Override
	public boolean isNotCreated() {
		return (! isCreated());
	}

	/**
	 * Manager
	 * @return	The condition

	 */
	@XmlTransient
	public boolean isManager() {
		return (isUserInRole("whosin","Manager"));
	}

	/**	 * {@link #isManager} negation.

	 * @return	The negated condition

	 */
	public boolean isNotManager() {
		return (! isManager());
	}
}
