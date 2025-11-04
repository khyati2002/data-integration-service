package com.salescode.dim.jooq.impl;



import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class SecondaryProduct extends com.salescode.dim.jooq.generated.tables.pojos.SecondaryProduct implements Serializable {
	private static final long serialVersionUID = 1L;

	/* we can store competitor name for other features like competitor focus product, SOS of competitors*/
	private String companyName;

	/* This specifies for which feature it is stored like focus brand ,competitor sku info ..etc*/
	private String feature;



	private String skuCode;
	private String batchCode;
	private String productCode;
	private String product;
	private String skuDescription;
	private String description;
	private String marketSkuCode;
	private String marketSku;
	private String category;
	private String categoryCode;
	private String subCategory;
	private String subCategoryCode;
	private String brand;
	private String brandCode;
	private String itemType;
	private String itemName;
	private String productTag;


	/* we can store feature specific information like minbaseqty value for bil */
	private Double quantity;


	private String outletCode;

	private String outletCategory;
	private String outletClass;
	private String account;
	private String channel;
	private String outletType;
	private String beat;
	private String filterKey;
	private String filterValue;
	private String loginid;


	private String locationHierarchy;


}
