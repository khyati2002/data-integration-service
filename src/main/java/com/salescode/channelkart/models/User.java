package com.salescode.channelkart.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salescode.channelkart.annotation.UniqueKey;
import com.salescode.channelkart.converters.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.OneToMany;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name="ck_user",
		indexes={
				@Index(name="ck_user_idx_1",columnList="loginid"),
				@Index(name="ck_user_idx_2",columnList="id")
		})
@SuppressWarnings({"java:S4144", "java:S1710", "java:S6353", "java:S1172", "java:S107"})
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(name = "all_user_hierarchy_procedure",
				procedureName = "hierarchycreationhirarchymetadata"
		),
		@NamedStoredProcedureQuery(name = "user_hierarchy_procedure",
				procedureName = "hierarchycreationhierarchymetadataindividualuser",
				parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, name = "loginids", type = String.class)
				})
})

public class User extends CommonDataModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1548094107145548143L;

	/**
	 * Common ID linking different login IDs
	 * For Eg - A Retailer might have multiple people in outlets to place order. This is a case where the outlet is very big
	 * and employs multiple assistants in the shop.
	 * In above case, there will be one userAccountId which is the retailer ID and multiple loginids for the assistants.
	 */
	@UniqueKey
	@Column(name = "useraccountid", length = 50, unique = true)
	@NotNull
	@Size(min = 1, max = 50)
	private String userAccountId;

	/**
	 * Login ID of the user
	 */
	@UniqueKey
	@Column(name = "loginid", length = 50, unique = true)
	@NotNull
	@Size(min = 1, max = 50)
	private String loginId;

	/**
	 * Password of the user
	 */
	@JsonIgnore
	@Column(name = "password", length = 100)
	@NotNull
	@Size(min = 4, max = 100)
	private String password;

	/**
	 * Email ID of the user
	 */
	@Column(name = "email", length = 50)
	@Size(min = 0, max = 50)
	@Email
	private String email;

	/**
	 * This column denotes the external unique identifier. If the client's database uses a different unique identifier in their system, and we cannot use that as our login ID, then we can put that information in this column.
	 */
	@Column(unique = true)
	private String externalReferenceId;

	/**
	 * Mobile number of the user
	 */
	@Column(name = "mobile", length = 15)
	@Pattern(regexp="(^[0-9]*$)")
	private String mobile;

	/**
	 * Id required to send mobile notifications to the user
	 */
	@Column(name = "usercontext", length = 500)
	@Size(min = 0, max = 500)
	private String userContext;

	/**
	 * Id required to send web notifications to the user
	 */
	@Column(name = "webcontext", length = 500)
	@Size(min = 0, max = 500)
	private String webContext;

	/**
	 * Date when the password what reset or changed
	 */
	@Column(name = "last_password_reset_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastPasswordResetDate;

	/**
	 * Store the last used passwords here and validate when needed
	 */
	@Column(name = "ck_lastusedpasswords", length = 500)
	@Transient
	private List<String> lastUsedPasswords;

	/**
	 * Roles assigned to the user. It is used for authorization to allow access to resources and actions.
	 * @see Role
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ck_user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "roles_id"))
	//@JsonSerialize(converter =RoleToStringConverter.class)
	//@JsonDeserialize(converter = StringToRoleConverter.class)
	private List<Role> roles;

	private String contactType;


	/**
	 * Dialcode for country
	 */
	private String countryCode;

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	/**
	 * Address of the user
	 */
	private String address;

	/**
	 * Reporting manager or Supervisor of the user. It can also be a Supplier or Distributor.
	 * In some cases, a user can have multiple reporting managers or Supervisors.
	 * In case of a retailer, multiple suppliers can be providing products to the retailer.
	 * @see HierarchyMetaData
	 */
	@JsonSerialize(converter = HierarchyMetaDataToStringConverter.class)
	@JsonDeserialize(using = HierarchyMetaDataListDeserializer.class)
//	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
//	@JoinTable(name = "ck_user_parent",
//			   joinColumns = @JoinColumn(name = "userloginid",referencedColumnName = "loginid"),
//	  		   inverseJoinColumns = @JoinColumn(name = "parent",referencedColumnName = "parent"),foreignKey = @javax.persistence
//	  		         .ForeignKey(value = ConstraintMode.NO_CONSTRAINT),inverseForeignKey = @javax.persistence
//	  		  		         .ForeignKey(value = ConstraintMode.NO_CONSTRAINT) )
	@Transient
	private List<HierarchyMetaData> immediateParent;

	/**
	 * The location of the user and its hierarchy
	 * For Eg - If users location is Bengaluru. The hierarchy would be Bengaluru~Karnataka~South~India
	 * @see Location
	 */
	@JsonSerialize(converter = LocationToStringConverter.class)
	@JsonDeserialize(using = LocationDeserializer.class)
	//@ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.PERSIST, CascadeType.MERGE})
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "location_hierarchy",
			referencedColumnName = "location_hierarchy"
	)
	private Location locationHierarchy;

	/**
	 * This field is specifically for Supplier designation. If a supplier needs to have a cap of total order,
	 * it can be set here. The minimum and maximum volume or value for the total order is updated.
	 * @see SupplierMetaData
	 */
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "user", orphanRemoval=true)
	@JsonManagedReference
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<SupplierMetaData> supplierMetaData= new ArrayList<>();

	/**
	 * The reporting hierarchy of the user
	 * For Eg - Retailer is handled by Supplier. And Supplier is handled by an ASM. So, the hierarchy would be
	 * Retailer~Supplier~ASM
	 */
	@Column(name = "hierarchy",columnDefinition = "LONGTEXT")
	@Size(min = 0, max = 5000)
	private String hierarchy;


	@Column(name = "normalized_hierarchy",columnDefinition = "LONGTEXT")
	@Size(min = 0, max = 5000)
	private String normalizedHierarchy;

	/**
	 * Designation of the user like Retailer, Supplier, ASM etc.
	 */

	@CollectionTable(name="ck_userdesignation",joinColumns=@JoinColumn(name = "loginId", referencedColumnName = "loginId"),indexes = {@Index(columnList = "loginId")})
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "designation")
	private Set<String> designation;

	@NotNull
	@NotBlank(message="name is a mandatory field and must not be empty")
	private String name;

	@Column(name = "registeredNumber", length = 15)
	@Pattern(regexp="(^[0-9]*$)")
	private String registeredNumber;

	private String facebookPSID;

	public String getAlternateId() {
		return alternateId;
	}

	public void setAlternateId(String alternateId) {
		this.alternateId = alternateId;
	}

	@Column(columnDefinition = "varchar(50)")
	private String alternateId;
	private String dialCode;

	/** The sso type. */
	@Column(columnDefinition = "varchar(100) default 'none'")
	private String ssoId;

	//@NotBlank(message=ValidationResponseMessage.NOTBLANK)
	private String deviceId;

	@Column(columnDefinition = "LONGTEXT")
	private String assignedHierarchy;

	private Boolean verified;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
	//@JsonDeserialize(converter = ClientTimeZoneStringToUTCDateConverter.class)
	private Date dob;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
	//@JsonDeserialize(converter = ClientTimeZoneStringToUTCDateConverter.class)
	private Date doa;

	private Boolean blocked;

	@JsonIgnore
	@Column(name = "report_password")
	private String reportPassword;

	public String getReportPassword() {
		return reportPassword;
	}

	public void setReportPassword(String reportPassword) {
		this.reportPassword = reportPassword;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public Date getDoa() {
		return doa;
	}

	public void setDoa(Date doa) {
		this.doa = doa;
	}

	/*
	 * Collection table for messenger information. This primary includes messenger defined id which
	 * we map with system user.
	 * facebookPSID, userContext falls under same category.
	 * */
//	@CollectionTable(name="ck_user_messenger_info",
//			joinColumns=@JoinColumn(
//					name = "loginId",
//					referencedColumnName = "loginId"
//			),
//			uniqueConstraints = @UniqueConstraint(
//					name= "uk_user_messenger_info",
//					columnNames= {"loginId","channel"}
//			),
//			foreignKey = @ForeignKey(
//					name= "FK_USER_LOGINID",
//					foreignKeyDefinition="Foreign key with User's loginid"
//			),
//			indexes = {
//					@Index(columnList = "loginId",name="idx_loginid"),
//					@Index(columnList = "channel",name="idx_channel"),
//					@Index(columnList = "loginId,channel",name="idx_loginid_channel", unique=true)
//			}
//	)
//	@ElementCollection(targetClass= UserMessengerInfo.class,fetch = FetchType.EAGER)
//	private Set<UserMessengerInfo> messengerInfo;

//	@Transient
//	private List<String> activeNotificationChannels;

	public User() {}

	public User(String userAccounId, String loginId, String password, String firstname, String lastname, String email, Boolean enabled,
				Date lastPasswordResetDate, List<Role> roles) {
		this.userAccountId = userAccounId;
		this.loginId = loginId;
		this.password = password;
		this.email = email;
		this.lastPasswordResetDate = lastPasswordResetDate;
		this.roles = roles;
	}

	/**
	 * @return the loginId
	 */
	public String getLoginId() {
		return loginId;
	}

	/**
	 * @param loginId the loginId to set
	 */
	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the mobile
	 */
	public String getMobile() {
		return mobile;
	}

	/**
	 * @param mobile the mobile to set
	 */
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	/**
	 * @return the userContext
	 */
	@JsonIgnore
	public String getUserContext() {
		return userContext;
	}

	/**
	 * @param userContext the userContext to set
	 */
	public void setUserContext(String userContext) {
		this.userContext = userContext;
	}

	/**
	 * @return the webContext
	 */
	public String getWebContext() {
		return webContext;
	}

	/**
	 * @param webContext the webContext to set
	 */
	public void setWebContext(String webContext) {
		this.webContext = webContext;
	}

	/**
	 * @return the lastPasswordResetDate
	 */
	public Date getLastPasswordResetDate() {
		return lastPasswordResetDate;
	}

	/**
	 * @param lastPasswordResetDate the lastPasswordResetDate to set
	 */
	public void setLastPasswordResetDate(Date lastPasswordResetDate) {
		this.lastPasswordResetDate = lastPasswordResetDate;
	}

	public List<String> getLastUsedPasswords() {
		return lastUsedPasswords;
	}

	public void setLastUsedPasswords(List<String> lastUsedPasswords) {
		this.lastUsedPasswords = lastUsedPasswords;
	}



	/**
	 * @return the roles
	 */
	public List<Role> getRoles() {
		return roles;
	}

	public boolean hasRole(String role) {
		if (roles != null) {
			return roles.stream()
					.anyMatch(r -> r.getName().equals(role));
		}
		return false;
	}



	public boolean hasDesignation(String toMatch) {
		if (designation != null) {
			return designation.stream()
					.anyMatch(d -> d.equals(toMatch));
		}
		return false;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	/**
	 * @return the userAccountId
	 */
	public String getUserAccountId() {
		return userAccountId;
	}

	/**
	 * @param userAccountId the userAccountId to set
	 */
	public void setUserAccountId(String userAccountId) {
		this.userAccountId = userAccountId;
	}

	/**
	 * @return the contactType
	 */
	public String getContactType() {
		return contactType;
	}

	/**
	 * @param contactType the contactType to set
	 */
	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the immediateParent
	 */
	public List<HierarchyMetaData> getImmediateParent() {
		if (this.immediateParent == null && this.hierarchy != null) {
			List<String> parents = findParents(hierarchy);
			List<HierarchyMetaData> hierarchyMetaDataList = parents.stream().map(parent -> {
				HierarchyMetaData hierarchyMetaData = new HierarchyMetaData();
				hierarchyMetaData.setImmediateParent(parent);
				return hierarchyMetaData;
			}).collect(Collectors.toList());
			this.setImmediateParent(hierarchyMetaDataList);
		}
		return immediateParent;
	}

	/**
	 * @param immediateParent the immediateParent to set
	 */
	public void setImmediateParent(List<HierarchyMetaData> immediateParent) {
		this.immediateParent = immediateParent;
	}

	/**
	 * @return the locationHierarchy
	 */
	public Location getLocationHierarchy() {
		return locationHierarchy;
	}

	/**
	 * @param locationHierarchy the locationHierarchy to set
	 */
	public void setLocationHierarchy(Location locationHierarchy) {
		this.locationHierarchy = locationHierarchy;
	}

	/**
	 * @return the supplierMetaData
	 */
	public List<SupplierMetaData> getSupplierMetaData() {
		return supplierMetaData;
	}

	/**
	 * @param supplierMetaData the supplierMetaData to set
	 */
	public void setSupplierMetaData(List<SupplierMetaData> supplierMetaData) {
		this.supplierMetaData = supplierMetaData;
	}

	/**
	 * @return the hierarchy
	 */
	@JsonIgnore
	public String getHierarchy() {
		return hierarchy;
	}

	/**
	 * @param hierarchy the hierarchy to set
	 */
	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	public static List<String> findParents(String hierarchy) {
		return Arrays.stream(hierarchy.split(","))
				.map(String::trim)
				.map(User::getImmediateParentFromHierarchy)
				.flatMap(Optional::stream)
				.distinct()
				.collect(Collectors.toList());
	}

	public static Optional<String> getImmediateParentFromHierarchy(String hierarchy) {
		String[] splitHierarchy = hierarchy.split(" > ");
		if (splitHierarchy.length > 1) {
			return Optional.of(splitHierarchy[1].trim());
		}
		return Optional.empty();
	}

	/**
	 * @return the designation
	 */
	public Set<String> getDesignation() {
		return designation;
	}

	/**
	 * @param designation the designation to set
	 */
	public void setDesignation(Set<String> designation) {
		this.designation = (designation!=null)?designation.stream().map(String::toLowerCase)
				.collect(Collectors.toSet()):null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRegisteredNumber() {
		return registeredNumber;
	}

	public void setRegisteredNumber(String registeredNumber) {
		this.registeredNumber = registeredNumber;
	}

	public String getFacebookPSID() {
		return facebookPSID;
	}

	public void setFacebookPSID(String facebookPSID) {
		this.facebookPSID = facebookPSID;
	}

	public String getDialCode() {
		return dialCode;
	}

	public void setDialCode(String dialCode) {
		this.dialCode = dialCode;
	}

	/**
	 * @return the ssoId
	 */
	public String getSsoId() {
		return ssoId;
	}

	/**
	 * @param ssoId the ssoId to set
	 */
	public void setSsoId(String ssoId) {
		this.ssoId = ssoId;
	}

	/**
	 * @return the deviceId
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Boolean isVerified() {
		return verified;
	}

	public Boolean getVerified() {
		return verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	@Override
	public int hashCode() {
		int result = ((loginId == null) ? 0 : loginId.hashCode());
		result = result + ((userAccountId == null) ? 0 : userAccountId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (loginId == null) {
			if (other.loginId != null)
				return false;
		} else if (!loginId.equals(other.loginId))
			return false;
		if (userAccountId == null) {
			if (other.userAccountId != null)
				return false;
		} else if (!userAccountId.equals(other.userAccountId))
			return false;
		return true;
	}


	public boolean canHash() {
		return true;
	}

	@Override
	public String toString() {
		return "User [userAccountId=" + userAccountId + ", loginId=" + loginId + "]";
	}

	public String getAssignedHierarchy() {
		return assignedHierarchy;
	}

	public User setAssignedHierarchy(String assignedHierarchy) {
		this.assignedHierarchy = assignedHierarchy;
		return this;
	}

	/**
	 * @return the messengerInfo
	 */
//	public Set<UserMessengerInfo> getMessengerInfo() {
//		return messengerInfo;
//	}
//
//	/**
//	 * @param messengerInfo the messengerInfo to set
//	 */
//	public void setMessengerInfo(Set<UserMessengerInfo> messengerInfo) {
//		this.messengerInfo = messengerInfo;
//	}
//
//	/**
//	 * Gets the active notification channels.
//	 *
//	 * @return the active notification channels
//	 */
//	public List<String> getActiveNotificationChannels() {
//		if(activeNotificationChannels == null) {
//			activeNotificationChannels= Arrays.stream(NotificationTypeRegistry.values())
//					.map(this::getChannel)
//					.filter(Objects::nonNull)
//					.map(m->m.toString().toLowerCase())
//					.collect(Collectors.toList());
//		}
//		return activeNotificationChannels;
//	}

//	@JsonIgnore
//	private NotificationTypeRegistry getChannel(NotificationTypeRegistry channel) {
//		switch(channel) {
//			case SMS:
//				if(StringUtils.isNotEmpty(mobile)) {
//					return channel;
//				}
//				break;
//
//			case EMAIL:
//				if(StringUtils.isNotEmpty(email)) {
//					return channel;
//				}
//				break;
//			case FIREBASE:
//				if(StringUtils.isNotEmpty(userContext)) {
//					return channel;
//				}
//				break;
//			default:
//				if(ObjectUtils.isNotEmpty(messengerInfo) &&
//						messengerInfo.stream().anyMatch(m->m.getChannel().equalsIgnoreCase(channel.name()))) {
//					return channel;
//				}
//		}
//		return null;
//	}
//
//	/**
//	 * Sets the active notification channels.
//	 *
//	 * @param activeNotificationChannels the new active notification channels
//	 */
//	public void setActiveNotificationChannels(List<String> activeNotificationChannels) {
//		this.activeNotificationChannels = activeNotificationChannels;
//	}

    @Override
	public String hash() {
		return loginId + super.hash();
	}

	/**
	 * @return the blocked
	 */
	public Boolean getBlocked() {
		return Boolean.TRUE.equals(blocked);
	}

	/**
	 * @param blocked the blocked to set
	 */
	public void setBlocked(Boolean blocked) {
		this.blocked = blocked;
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

	public String getExternalReferenceId() {
		return externalReferenceId;
	}

	public User setExternalReferenceId(String externalReferenceId) {
		this.externalReferenceId = externalReferenceId;
		return this;
	}
}