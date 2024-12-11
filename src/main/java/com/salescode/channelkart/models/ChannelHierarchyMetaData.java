package com.salescode.channelkart.models;


import com.salescode.channelkart.annotation.UniqueKey;

import javax.persistence.Column;
import java.io.Serializable;

public class ChannelHierarchyMetaData implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * Login ID of supplier or stockist who is directly supplying to the outlet. 
	 * For Eg - Retailer is handled by the Supplier and hence level1Supplier of retailer is Supplier.
	 */
	@UniqueKey(nativeName="level1supplier")
	@Column(name = "level1supplier", length = 100, unique = true)
	public String level1Supplier;
	
	public String level1SupplierName;

	/**
	 * Login ID of supplier or stockist who is indirectly handling the outlet. 
	 * For Eg - Retailer is handled by the substockist and hence level2Supplier of retailer is Supplier and level1Supplier of
	 * retailer is the subStockist.
	 */
	@UniqueKey(nativeName="level2supplier")
	@Column(name = "level2supplier", length = 100, unique = true)
	public String level2Supplier;
	
	public String level2SupplierName;

	/**
	 * Login ID of supplier or stockist who is indirectly handling the outlet. 
	 * For Eg - Retailer is handled by the substockist and hence level3Supplier of retailer is Supplier and level2Supplier of
	 * retailer is the subStockist1 and level1Supplier of retailer is the subStockist.
	 */
	@UniqueKey(nativeName="level3supplier")
	@Column(name = "level3supplier", length = 100, unique = true)
	public String level3Supplier;
	
	public String level3SupplierName;

	/**
	 * @return the level1Supplier
	 */
	public String getLevel1Supplier() {
		return level1Supplier;
	}

	/**
	 * @param level1Supplier the level1Supplier to set
	 */
	public void setLevel1Supplier(String level1Supplier) {
		this.level1Supplier = level1Supplier;
	}

	/**
	 * @return the level1SupplierName
	 */
	public String getLevel1SupplierName() {
		return level1SupplierName;
	}

	/**
	 * @param level1SupplierName the level1SupplierName to set
	 */
	public void setLevel1SupplierName(String level1SupplierName) {
		this.level1SupplierName = level1SupplierName;
	}

	/**
	 * @return the level2Supplier
	 */
	public String getLevel2Supplier() {
		return level2Supplier;
	}

	/**
	 * @param level2Supplier the level2Supplier to set
	 */
	public void setLevel2Supplier(String level2Supplier) {
		this.level2Supplier = level2Supplier;
	}

	/**
	 * @return the level2SupplierName
	 */
	public String getLevel2SupplierName() {
		return level2SupplierName;
	}

	/**
	 * @param level2SupplierName the level2SupplierName to set
	 */
	public void setLevel2SupplierName(String level2SupplierName) {
		this.level2SupplierName = level2SupplierName;
	}

	/**
	 * @return the level3Supplier
	 */
	public String getLevel3Supplier() {
		return level3Supplier;
	}

	/**
	 * @param level3Supplier the level3Supplier to set
	 */
	public void setLevel3Supplier(String level3Supplier) {
		this.level3Supplier = level3Supplier;
	}

	/**
	 * @return the level3SupplierName
	 */
	public String getLevel3SupplierName() {
		return level3SupplierName;
	}

	/**
	 * @param level3SupplierName the level3SupplierName to set
	 */
	public void setLevel3SupplierName(String level3SupplierName) {
		this.level3SupplierName = level3SupplierName;
	}

	@Override
	public int hashCode() {
		int result = ((level1Supplier == null) ? 0 : level1Supplier.hashCode());
		result = result + ((level2Supplier == null) ? 0 : level2Supplier.hashCode());
		result = result + ((level3Supplier == null) ? 0 : level3Supplier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		ChannelHierarchyMetaData other = (ChannelHierarchyMetaData) obj;
		if (level1Supplier == null) {
			if (other.level1Supplier != null)
				return false;
		} else if (!level1Supplier.equals(other.level1Supplier))
			return false;
		if (level2Supplier == null) {
			if (other.level2Supplier != null)
				return false;
		} else if (!level2Supplier.equals(other.level2Supplier))
			return false;
		if (level3Supplier == null) {
			if (other.level3Supplier != null)
				return false;
		} else if (!level3Supplier.equals(other.level3Supplier))
			return false;
		return true;
	}
	
	
}
