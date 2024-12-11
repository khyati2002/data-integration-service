package com.salescode.channelkart.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.salescode.channelkart.annotation.UniqueKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Entity
@Table(name="ck_location",
indexes={
		@Index(name="Location_idx_1",columnList="location_hierarchy")
})
public class Location extends CommonDataModel {

	private static final long serialVersionUID = -8105119419691406049L;

	//@Column(name = "location_hierarchy", length = 200, unique = true)
	@UniqueKey
	@Column(name = "location_hierarchy", unique = true, columnDefinition="varchar(500)")
	@Size(min = 1, max = 500)
	private String locationHierarchy;
	private String name;
	private String area;
	private String pincode;
	private String territory;
	private String town;
	private String city;
	private String state;
	private String region;
	private String zone;
	private String cluster;
	private String branch;
	private String country;
	private String areacode;
	private String territoryCode;
	private String cityCode;
	private String stateCode;
	private String regionCode;
	private String zoneCode;
	private String clusterCode;
	private String branchCode;
	private String countryCode;
	private String locationType;
	private String locationName;

	private String district;
	private String districtCode;
	private String townCode;

	private String salescodeId;


	public String getDistrict() {
		return district;
	}
	public void setDistrict(String district) {
		this.district = district;
	}
	public String getDistrictCode() {
		return districtCode;
	}
	public void setDistrictCode(String districtCode) {
		this.districtCode = districtCode;
	}
	public String getTownCode() {
		return townCode;
	}
	public void setTownCode(String townCode) {
		this.townCode = townCode;
	}
	/**
	 * @return the locationHierarchy
	 */
	public String getLocationHierarchy() {
		return locationHierarchy;
	}
	/**
	 * @param hierarchy the locationHierarchy to set
	 */
	public void setLocationHierarchy(String locationHierarchy) {
		this.locationHierarchy = locationHierarchy;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the area
	 */
	public String getArea() {
		return area;
	}
	/**
	 * @param area the area to set
	 */
	public void setArea(String area) {
		this.area = area;
	}
	/**
	 * @return the pincode
	 */
	public String getPincode() {
		return pincode;
	}
	/**
	 * @param pincode the pincode to set
	 */
	public void setPincode(String pincode) {
		this.pincode = pincode;
	}
	/**
	 * @return the territory
	 */
	public String getTerritory() {
		return territory;
	}
	/**
	 * @param territory the territory to set
	 */
	public void setTerritory(String territory) {
		this.territory = territory;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}
	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}
	/**
	 * @return the zone
	 */
	public String getZone() {
		return zone;
	}
	/**
	 * @param zone the zone to set
	 */
	public void setZone(String zone) {
		this.zone = zone;
	}
	/**
	 * @return the cluster
	 */
	public String getCluster() {
		return cluster;
	}
	/**
	 * @param cluster the cluster to set
	 */
	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
	/**
	 * @return the branch
	 */
	public String getBranch() {
		return branch;
	}
	/**
	 * @param branch the branch to set
	 */
	public void setBranch(String branch) {
		this.branch = branch;
	}
	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return the areacode
	 */
	public String getAreacode() {
		return areacode;
	}
	/**
	 * @param areacode the areacode to set
	 */
	public void setAreacode(String areacode) {
		this.areacode = areacode;
	}
	/**
	 * @return the territoryCode
	 */
	public String getTerritoryCode() {
		return territoryCode;
	}
	/**
	 * @param territoryCode the territoryCode to set
	 */
	public void setTerritoryCode(String territoryCode) {
		this.territoryCode = territoryCode;
	}
	/**
	 * @return the cityCode
	 */
	public String getCityCode() {
		return cityCode;
	}
	/**
	 * @param cityCode the cityCode to set
	 */
	public void setCityCode(String cityCode) {
		this.cityCode = cityCode;
	}
	/**
	 * @return the stateCode
	 */
	public String getStateCode() {
		return stateCode;
	}
	/**
	 * @param stateCode the stateCode to set
	 */
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}
	/**
	 * @return the regionCode
	 */
	public String getRegionCode() {
		return regionCode;
	}
	/**
	 * @param regionCode the regionCode to set
	 */
	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}
	/**
	 * @return the zoneCode
	 */
	public String getZoneCode() {
		return zoneCode;
	}
	/**
	 * @param zoneCode the zoneCode to set
	 */
	public void setZoneCode(String zoneCode) {
		this.zoneCode = zoneCode;
	}
	/**
	 * @return the clusterCode
	 */
	public String getClusterCode() {
		return clusterCode;
	}
	/**
	 * @param clusterCode the clusterCode to set
	 */
	public void setClusterCode(String clusterCode) {
		this.clusterCode = clusterCode;
	}
	/**
	 * @return the branchCode
	 */
	public String getBranchCode() {
		return branchCode;
	}
	/**
	 * @param branchCode the branchCode to set
	 */
	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}
	/**
	 * @return the countryCode
	 */
	public String getCountryCode() {
		return countryCode;
	}
	/**
	 * @param countryCode the countryCode to set
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	/**
	 * @return the locationType
	 */
	public String getLocationType() {
		return locationType;
	}
	/**
	 * @param locationType the locationType to set
	 */
	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}
	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}
	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	/**
	 * @return the town
	 */
	public String getTown() {
		return town;
	}
	/**
	 * @param town the town to set
	 */
	public void setTown(String town) {
		this.town = town;
	}

	public String getSalescodeId() {
		return salescodeId;
	}

	public void setSalescodeId(String salescodeId) {
		this.salescodeId = salescodeId;
	}

	@Override
	public String toString() {
		return "Location [locationHierarchy=" + locationHierarchy + "]";
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Location)) return false;
		if (!super.equals(o)) return false;
		Location location = (Location) o;
		return Objects.equals(getLocationHierarchy(), location.getLocationHierarchy()) && Objects.equals(getName(), location.getName()) && Objects.equals(getArea(), location.getArea()) && Objects.equals(getPincode(), location.getPincode()) && Objects.equals(getTerritory(), location.getTerritory()) && Objects.equals(getTown(), location.getTown()) && Objects.equals(getCity(), location.getCity()) && Objects.equals(getState(), location.getState()) && Objects.equals(getRegion(), location.getRegion()) && Objects.equals(getZone(), location.getZone()) && Objects.equals(getCluster(), location.getCluster()) && Objects.equals(getBranch(), location.getBranch()) && Objects.equals(getCountry(), location.getCountry()) && Objects.equals(getAreacode(), location.getAreacode()) && Objects.equals(getTerritoryCode(), location.getTerritoryCode()) && Objects.equals(getCityCode(), location.getCityCode()) && Objects.equals(getStateCode(), location.getStateCode()) && Objects.equals(getRegionCode(), location.getRegionCode()) && Objects.equals(getZoneCode(), location.getZoneCode()) && Objects.equals(getClusterCode(), location.getClusterCode()) && Objects.equals(getBranchCode(), location.getBranchCode()) && Objects.equals(getCountryCode(), location.getCountryCode()) && Objects.equals(getLocationType(), location.getLocationType()) && Objects.equals(getLocationName(), location.getLocationName()) && Objects.equals(getDistrict(), location.getDistrict()) && Objects.equals(getDistrictCode(), location.getDistrictCode()) && Objects.equals(getTownCode(), location.getTownCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getLocationHierarchy(), getName(), getArea(), getPincode(), getTerritory(), getTown(), getCity(), getState(), getRegion(), getZone(), getCluster(), getBranch(), getCountry(), getAreacode(), getTerritoryCode(), getCityCode(), getStateCode(), getRegionCode(), getZoneCode(), getClusterCode(), getBranchCode(), getCountryCode(), getLocationType(), getLocationName(), getDistrict(), getDistrictCode(), getTownCode());
	}
}
