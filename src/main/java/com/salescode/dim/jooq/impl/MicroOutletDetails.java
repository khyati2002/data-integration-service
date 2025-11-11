package com.salescode.dim.jooq.impl;

import com.fasterxml.jackson.annotation.JsonInclude;


//import com.salescode.dim.utils.GeoUtils;
import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.jooq.Geometry;
import org.locationtech.jts.geom.Point;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class MicroOutletDetails extends com.salescode.dim.jooq.generated.tables.pojos.OutletDetails implements Serializable {

    private static final long serialVersionUID = 5919494859608500774L;

    /**
     * Unique code of the outlet or store. It can also be the retailer code if there is no separate
     * code for the shop
     */
    private String outletcode;

    private String location;

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
    private String contactno;

    /**
     * Address to be displayed in the profile
     */
    private String displayAddress;

    /**
     * If the location of the outlet has been captured or not. If the latitude/longitude are captured, then it
     * is mapped. Otherwise it is not.
     */
    private boolean mapped;

    /**
     * The weeks in the month that the salesperson has to visit the outlet.
     * For Eg - 1,3 means it will be visited in the 1st and 3rd week of the month.
     */
    private String frequency;

    /**
     * Channel of the outlet
     * For Eg - Retail or Wholesale etc.
     */
    private String channel;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String outletCategory;

    private String outletClass;

    private String account;

    private String tinNo;

    private String gstNo;

    private String marketName;

    private String marketId;

    private String loginid;

    private String subChannel;

    private String distributionChannel;

    private String outletDivision;
//
//    @JsonIgnore
//    private Geometry coordinate;

    private String hierarchy;

    // Getters and Setters

    public String getSubChannel() {
        return subChannel;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }

    public String getDistributionChannel() {
        return distributionChannel;
    }

    public void setDistributionChannel(String distributionChannel) {
        this.distributionChannel = distributionChannel;
    }

    public String getOutletDivision() {
        return outletDivision;
    }

    public void setOutletDivision(String outletDivision) {
        this.outletDivision = outletDivision;
    }

    public String getLoginid() {
        return loginid;
    }

    public void setLoginid(String loginid) {
        this.loginid = loginid;
    }

//    public Geometry getCoordinate() {
//        return coordinate;
//    }
//
//    public void setCoordinate(Geometry coordinate) {
//        this.coordinate = coordinate;
//    }

    public String getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public String getOutletCategory() {
        return outletCategory;
    }

    public void setOutletCategory(String outletCategory) {
        this.outletCategory = outletCategory;
    }

    public String getOutletClass() {
        return outletClass;
    }

    public void setOutletClass(String outletClass) {
        this.outletClass = outletClass;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getTinNo() {
        return tinNo;
    }

    public void setTinNo(String tinNo) {
        this.tinNo = tinNo;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getOutletcode() {
        return outletcode;
    }

    public void setOutletCode(String outletcode) {
        this.outletcode = outletcode;
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

    public boolean isMapped() {
        return mapped;
    }

    public void setMapped(boolean mapped) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationHierarchy() {
        return getLocation();
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
//        setCoordinate(GeoUtils.toGeoPint(latitude, longitude));
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
//        setCoordinate(GeoUtils.toGeoPint(latitude, longitude));
    }

    @Override
    public int hashCode() {
        int result = ((outletcode == null) ? 0 : outletcode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        MicroOutletDetails other = (MicroOutletDetails) obj;
        if (outletcode == null) {
            return other.outletcode == null;
        } else return outletcode.equals(other.outletcode);
    }

    @Override
    public String toString() {
        return "OutletDetails [outletCode=" + outletcode + "," + "outletName = " + outletName
                + ", channel=" + channel + "]";
    }

    @Override
    public boolean canHash() {
        return true;
    }
}