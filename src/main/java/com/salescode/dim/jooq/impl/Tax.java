package com.salescode.dim.jooq.impl;

import java.io.Serializable;
import java.util.Date;



public class Tax extends com.salescode.dim.jooq.generated.tables.pojos.Tax implements Serializable {

	private static final long serialVersionUID = -2806558689513245242L;

	/** A unique code in case of duplicate SKUs with different MRP. */
	private String batchCode;

	/** SKU is the lowest level of the hierarchy */
	private String skuCode;

	/** Rate of tax on product like 9% */
	private double taxRate;

	/** Type of tax like IGST,CGST,VAT,CESS */
	private String taxType;

	/** For State Wise Tax */
	private String state;

	private String taxGroup;

	private int priority;

	public Tax() {
	}

	public Tax(String batchCode, double taxRate, String taxType, String state, Date startDate) {
		this.batchCode = batchCode;
		this.taxRate = taxRate;
		this.taxType = taxType;
		this.state = state;
		this.startDate = startDate;
	}
	private Date startDate;

	private Date endDate;

	public String getTaxGroup() {
		return taxGroup;
	}

	public void setTaxGroup(String taxGroup) {
		this.taxGroup = taxGroup;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @return the batchCode
	 */
	public String getBatchCode() {
		return batchCode;
	}

	/**
	 * @param batchCode the batchCode to set
	 */
	public void setBatchCode(String batchCode) {
		this.batchCode = batchCode;
	}

	/**
	 * @return the skuCode
	 */
	public String getSkuCode() {
		return skuCode;
	}

	/**
	 * @param skuCode the skuCode to set
	 */
	public void setSkuCode(String skuCode) {
		this.skuCode = skuCode;
	}

	/**
	 * @return the taxRate
	 */
//	public double getTaxRate() {
//		return taxRate;
//	}

	/**
	 * @param taxRate the taxRate to set
	 */
	public void setTaxRate(double taxRate) {
		this.taxRate = taxRate;
	}

	/**
	 * @return the taxType
	 */
//	public String getTaxType() {
//		return taxType;
//	}

	/**
	 * @param taxType the taxType to set
	 */
	public void setTaxType(String taxType) {
		this.taxType = taxType;
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


//	public Date getStartDate() {
//		return startDate;
//	}

//	public void setStartDate(Date startDate) {
//		this.startDate = startDate;
//	}

//	public Date getEndDate() {
//		return endDate;
//	}
//
//	public void setEndDate(Date endDate) {
//		this.endDate = endDate;
//	}
}
