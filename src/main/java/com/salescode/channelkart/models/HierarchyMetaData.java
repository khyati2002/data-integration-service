package com.salescode.channelkart.models;



import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.salescode.channelkart.annotation.UniqueKey;
import com.salescode.channelkart.converters.LocationHierarchyDeserializer;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Entity
@Table(name="ck_hierarchy_metadata"
,uniqueConstraints = {@UniqueConstraint(name = "uk_hierarchy_metadata",columnNames = { "parent","hierarchy" })},
indexes={
		@Index(name="ck_hierarchy_metadata_idx_1",columnList="parent"),
		@Index(name="ck_hierarchy_metadata_idx_2",columnList="id")
})
public class HierarchyMetaData extends CommonDataModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1747310645850379416L;

	/**
	 * Login ID of immediate parent or the reporting persor or the supervisor or supplier. Anybody to whom the user reports
	 * or is handled by. For Eg - Retailer is handled by the Supplier and hence immediateParent of retailer is Supplier.
	 */
	@Column(name = "parent", length = 100)
	public String immediateParent;
	
	/**
	 * The reporting hierarchy of the user
	 * For Eg - Retailer is handled by Supplier. And Supplier is handled by an ASM. So, the hierarchy would be
	 * Retailer~Supplier~ASM
	 */
	@UniqueKey
	@Column(name = "hierarchy", length = 750, columnDefinition="varchar(750)")
	//@Column(name = "hierarchy", columnDefinition="LONGTEXT")
	@Size(min = 0, max = 750)
	public String hierarchy;
	
	/**
	 * The location of the user and its hierarchy
	 * For Eg - If users location is Bengaluru. The hierarchy would be Bengaluru~Karnataka~South~India
	 * @see com.applicate.services.channelkart.models.Location
	 */
	@JsonDeserialize(using = LocationHierarchyDeserializer.class)
	@Column(name="location_hierarchy",
			columnDefinition = "VARCHAR(200) REFERENCES ck_location(location_hierarchy)")
	public String locationHierarchy;
	
	/**
	 * @return the immediateParent
	 */
	public String getImmediateParent() {
		return immediateParent;
	}
	/**
	 * @param immediateParent the immediateParent to set
	 */
	public void setImmediateParent(String immediateParent) {
		this.immediateParent = immediateParent;
	}
	/**
	 * @return the hierarchy
	 */
	public String getHierarchy() {
		return hierarchy;
	}
	/**
	 * @param hierarchy the hierarchy to set
	 */
	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}
	/**
	 * @return the locationHierarchy
	 */
	public String getLocationHierarchy() {
		return locationHierarchy;
	}
	/**
	 * @param locationHierarchy the locationHierarchy to set
	 */
	public void setLocationHierarchy(String locationHierarchy) {
		this.locationHierarchy = locationHierarchy;
	}
	
	@Override
	public String toString() {
		return "HierarchyMetaData [id= "+super.getId()+" immediateParent=" + immediateParent + ", hierarchy=" + hierarchy
				+ ", locationHierarchy=" + locationHierarchy + "]";
	}
	
}
