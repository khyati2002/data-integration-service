package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class Sales extends com.salescode.dim.jooq.generated.tables.pojos.Sales implements Serializable {
	private static final long serialVersionUID = 1L;

	private String loginId;
	
	private String outletCode;



	private String invoiceNumber;
	private String referenceNumber;

	private float totalQuantity;
	
	private float totalInitialQuantity;
	
	private float normalizedQuantity;
	
	private float initialNormalizedQuantity;



	private List<SalesDetails> salesDetails;

}
