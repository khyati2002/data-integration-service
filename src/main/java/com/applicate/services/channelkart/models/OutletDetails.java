package com.applicate.services.channelkart.models;


import com.applicate.services.channelkart.converters.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.applicate.services.channelkart.annotation.UniqueKey;
import com.applicate.services.channelkart.dto.SupplierInfo;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;


@Entity
@Table(name="ck_outlet_details",
indexes={
		@Index(name="ck_outlet_details_idx_1",columnList="outletCode"),
		@Index(name="ck_outlet_details_idx_2",columnList="id,version"),
		@Index(name="ck_outlet_details_idx_3",columnList="controlGroup")
})
@JsonInclude(Include.NON_NULL)
public class OutletDetails extends CommonDataModel {
	private static final long serialVersionUID = 1L;

	/**
	 * Unique code of the outlet or store. It can also be the retailer code if there is no separate
	 * code for the shop
	 */
	@UniqueKey
	@Column(name = "outletcode", length = 200, unique = true)
	@NotNull
	@Size(min = 1, max = 500)
//	@NotBlank(message=ValidationResponseMessage.NOTBLANK)
//	@Length(min=1, max=500,message=ValidationResponseMessage.LENGTH)
	private String outletCode;


	/**
	 * The retailer or handler of the outlet. The user will have a login id to use the App or Portal.
	 * In some cases, the order is taken by salesperson visiting the outlets and not the retailer. In such cases,
	 * this field is populated with salesperson
	 * @see User
	 */
	@JsonSerialize(converter = UserToStringConverter.class)
	@JsonDeserialize(using = UserCreateDeserializer.class)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "loginid",
			referencedColumnName = "loginid"
	)
	private User userName;

	/**
	 * If the outlet is being visited by a salesperson and order is taken by the salesperson, then there
	 * will be a defined route or beat. This route or beat comprises of outlets to be visited in that route.
	 */
	private String beatName;

	/**
	 * Code of the beat assigned to the salesperson
	 */
	private String beat;

	/**
	 * Name of the outlet
	 */
	private String outletName;

	@JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastOrderDate;

	/**
	 * Type of outlet
	 * For Eg - Grocery, Food chain
	 */
	private String outletType;

	/**
	 * Address of the outlet
	 */
	private String address;

	/**
	 * Contact person of the outlet
	 */
	private String contactName;

	/**
	 * Mobile number of the outlet
	 */
	//@NotEmpty
	@Pattern(regexp="(^[0-9]*$)")
	private String contactno;

	/**
	 * Address to be displayed in the profile
	 */
	private String displayAddress;

	/**
	 * If the location of the outlet has been captured or not. If the latitude/longitude are captured, then it
	 * is mapped. Otherwise it is not.
	 */
	private Boolean mapped;

	/**
	 * The weeks in the month that the salesperson has to visit the outlet.
	 * For Eg - 1,3 means it will be visited in the 1st and 3rd week of the month.
	 */
	private String frequency;

	@JsonDeserialize(using = LocationDeserializer.class)
	@JsonSerialize(converter = LocationToStringConverter.class)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "location_hierarchy",
			referencedColumnName = "location_hierarchy"
	)
	private Location location;

	/**
	 * Channel of the outlet
	 * For Eg - Retail or Wholesale etc.
	 */
	private String channel;

	/**
	 * Reporting manager or Supervisor of the outlet. It can also be a Supplier or Distributor.
	 * In some cases, an outlet can have multiple suppliers who can be providing products to the retailer.
	 * @see HierarchyMetaData
	 */
	@JsonSerialize(converter = HierarchyMetaDataToStringConverter.class)
	@JsonDeserialize(converter = StringToHierarchyMetaDataConverter.class)
	@ManyToMany(cascade= {CascadeType.MERGE},fetch = FetchType.EAGER)
	@JoinTable(name = "ck_outlet_details_hierarchymetadata",
			joinColumns = @JoinColumn(name = "outlet_id"),
			inverseJoinColumns = @JoinColumn(name = "hierarchy_metadata_id"))
	private List<HierarchyMetaData> immediateParent;

	@Column(columnDefinition = "Decimal(10,8)")
	private Double latitude;

	@Column(columnDefinition = "Decimal(11,8)")
	private Double longitude;

	private String email;

	private String blobKey;

	private String outletCategory;
	
	private String outletClass;
	
	private String account;
	
	private String tinNo;
	
	private String gstNo;
	
	private String marketName;
	
	private String marketId;

	@Column(name = "hierarchy",columnDefinition = "LONGTEXT")
	@Size(min = 0, max = 3000)
	private String hierarchy;
	
	@Column(name = "normalized_hierarchy",columnDefinition = "LONGTEXT")
	@Size(min = 0, max = 3000)
	private String normalizedHierarchy;

	private String soldTo;

	private String outletDivision;

	private String distributionChannel;

	private String subChannel;

	private String subTerritory;

	private String controlGroup;

	private String priceListId;

	public String getSoldTo() {
		return soldTo;
	}

	public void setSoldTo(String soldTo) {
		this.soldTo = soldTo;
	}

	public String getOutletDivision() {
		return outletDivision;
	}

	public void setOutletDivision(String outletDivision) {
		this.outletDivision = outletDivision;
	}

	public String getDistributionChannel() {
		return distributionChannel;
	}

	public void setDistributionChannel(String distributionChannel) {
		this.distributionChannel = distributionChannel;
	}

	public String getSubChannel() {
		return subChannel;
	}

	public void setSubChannel(String subChannel) {
		this.subChannel = subChannel;
	}

	public String getSubTerritory() {
		return subTerritory;
	}

	public void setSubTerritory(String subTerritory) {
		this.subTerritory = subTerritory;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
	private Date doo;

	public Date getDoo() {
		return doo;
	}

	public void setDoo(Date doo) {
		this.doo = doo;
	}


//	public Point getCoordinate() {
//		return coordinate;
//	}
//
//	public void setCoordinate(Point coordinate) {
//		this.coordinate = coordinate;
//	}
//
//	@JsonIgnore
//	private Point coordinate;


	/**
	 * @return the outletCategory
	 */
	public String getOutletCategory() {
		return outletCategory;
	}

	/**
	 * @param outletCategory the outletCategory to set
	 */
	public void setOutletCategory(String outletCategory) {
		this.outletCategory = outletCategory;
	}

	/**
	 * @return the outletClass
	 */
	public String getOutletClass() {
		return outletClass;
	}

	/**
	 * @param outletClass the outletClass to set
	 */
	public void setOutletClass(String outletClass) {
		this.outletClass = outletClass;
	}

	/**
	 * @return email of outlet
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return blob key
	 */
	public String getBlobKey() {
		return blobKey;
	}

	/**
	 * @param blobKey blob key to set
	 */
	public void setBlobKey(String blobKey) {
		this.blobKey = blobKey;
	}
	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * @return the tinNo
	 */
	public String getTinNo() {
		return tinNo;
	}

	/**
	 * @param tinNo the tinNo to set
	 */
	public void setTinNo(String tinNo) {
		this.tinNo = tinNo;
	}

	/**
	 * @return the gstNo
	 */
	public String getGstNo() {
		return gstNo;
	}

	/**
	 * @param gstNo the gstNo to set
	 */
	public void setGstNo(String gstNo) {
		this.gstNo = gstNo;
	}

	/**
	 * @return the marketName
	 */
	public String getMarketName() {
		return marketName;
	}

	/**
	 * @param marketName the marketName to set
	 */
	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}

	/**
	 * @return the marketId
	 */
	public String getMarketId() {
		return marketId;
	}

	/**
	 * @param marketId the marketId to set
	 */
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public User getRetailerInfo() {
		return retailerInfo;
	}

	public void setRetailerInfo(User retailerInfo) {
		this.retailerInfo = retailerInfo;
	}

	/**
	 * transient field to access the minimumretailer information
	 */
	@Transient
	private User retailerInfo;

	/**
	 * transient field to help upload of users with outlets.
	 */
	@Transient
	private List<List<User>> users;
	
	/**
	 * transient field to get details of supplier.
	 */
	@Transient
	private List<SupplierInfo> suppliers;
	
	/**
	 * transient field to get details of salesRep.
	 */
//	@Transient
//	private List<UserInfo> salesReps;

	/**
	 * transient field for Perfetti minimal response output.
	 */
	@Transient
	private String geoDist;

//	@Transient
//	private List<OutletDetailsShipToDTO> shiptoOutlets;

	public List<SupplierInfo> getSuppliers() {
		return suppliers;
	}

	public void setSuppliers(List<SupplierInfo> suppliers) {
		this.suppliers = suppliers;
	}

//	public List<UserInfo> getSalesReps() {
//		return salesReps;
//	}
//
//	public void setSalesReps(List<UserInfo> salesReps) {
//		this.salesReps = salesReps;
//	}

	public String getOutletCode() {
		return outletCode;
	}

	public void setOutletCode(String outletCode) {
		this.outletCode = outletCode;
	}

	public User getUserName() {
		return userName;
	}

	public void setUserName(User userName) {
		this.userName = userName;
	}

	public String getBeatName() {
		return beatName;
	}

	public void setBeatName(String beatName) {
		this.beatName = beatName;
	}

	public String getBeat() {
		return beat;
	}

	public void setBeat(String beat) {
		this.beat = beat;
	}

	public String getOutletName() {
		return outletName;
	}

	public void setOutletName(String outletName) {
		this.outletName = outletName;
	}

	public String getOutletType() {
		return outletType;
	}

	public void setOutletType(String outletType) {
		this.outletType = outletType;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactno() {
		return contactno;
	}

	public void setContactno(String contactno) {
		this.contactno = contactno;
	}

	public String getDisplayAddress() {
		return displayAddress;
	}

	public void setDisplayAddress(String displayAddress) {
		this.displayAddress = displayAddress;
	}

	public Boolean getMapped() {
		return mapped;
	}

	public Boolean isMapped() {
		return getMapped();
	}

	public void setMapped(Boolean mapped) {
		this.mapped = mapped;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}


	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	@JsonIgnore
	public Location getLocationHierarchy() {
		return getLocation();
	}

	public void setLocationHierarchy(Location location) {
		setLocation(location);
	}

	/**
	 * @return the immediateParent
	 */
	public List<HierarchyMetaData> getImmediateParent() {
		return immediateParent;
	}

	/**
	 * @param immediateParent the immediateParent to set
	 */
	public void setImmediateParent(List<HierarchyMetaData> immediateParent) {
		this.immediateParent = immediateParent;
	}

	/**
	 * @return the lastOrderDate
	 */
	public Date getLastOrderDate() {
		return lastOrderDate;
	}

	/**
	 * @param lastOrderDate the lastOrderDate to set
	 */
	public void setLastOrderDate(Date lastOrderDate) {
		this.lastOrderDate = lastOrderDate;
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return ((outletCode == null) ? 0 : outletCode.hashCode());
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		OutletDetails other = (OutletDetails) obj;
		if (outletCode == null) {
			if (other.outletCode != null)
				return false;
		} else if (!outletCode.equals(other.outletCode))
			return false;
		return true;
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	public List<List<User>> getUsers() {
		return users;
	}

	public void setUsers(List<List<User>> users) {
		this.users = users;
	}

	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
		//setCoordinate(GeoUtils.toGeoPint(latitude,longitude));
	}

	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
		//setCoordinate(GeoUtils.toGeoPint(latitude,longitude));
	}

	public String getControlGroup() {
		return controlGroup;
	}

	public void setControlGroup(String controlGroup) {
		this.controlGroup = controlGroup;
	}

	public String getGeoDist() {
		return geoDist;
	}

	public void setGeoDist(String geoDist) {
		this.geoDist = geoDist;
	}

	@Override
	public String toString() {
		return "OutletDetails [outletCode=" + outletCode + ", userName=" + userName + ", outletName=" + outletName
				+ ", channel=" + channel + "]";
	}

	@Override
	public boolean canHash() {
		return true;
	}

	@Override
	public boolean forceHash(){
		return this.getImmediateParent() != null && this.getImmediateParent().stream()
				.anyMatch(CommonDataModel::isCreate);
	}

	/**
	 * @return the normalizedHierarchy
	 */
	public String getNormalizedHierarchy() {
		return normalizedHierarchy;
	}

	/**
	 * @param normalizedHierarchy the normalizedHierarchy to set
	 */
	public void setNormalizedHierarchy(String normalizedHierarchy) {
		this.normalizedHierarchy = normalizedHierarchy;
	}

	public String getPriceListId() {
		return priceListId;
	}

	public void setPriceListId(String priceListId) {
		this.priceListId = priceListId;
	}

//	public List<OutletDetailsShipToDTO> getShiptoOutlets() {
//		return shiptoOutlets;
//	}
//
//	public void setShiptoOutlets(List<OutletDetailsShipToDTO> shiptoOutlets) {
//		this.shiptoOutlets = shiptoOutlets;
//	}
}
