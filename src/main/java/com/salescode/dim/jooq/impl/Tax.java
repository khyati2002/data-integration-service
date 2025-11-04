package com.salescode.dim.jooq.impl;


import lombok.Data;


import java.io.Serializable;
import java.util.Date;



@Data
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

	private Date startDate;

	private Date endDate;


}
