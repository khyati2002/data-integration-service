package com.salescode.channelkart.models.enums;

public enum Sequence {

	PRODUCT_DETAILS_BRAND("product_brand"),
	PRODUCT_DETAILS_CATEGORY("product_category"),
	PRODUCT_DETAILS_SUB_CATEGORY("product_subcategory"),
	PRODUCT_DETAILS_MARKET_SKU_CODE("product_msku"),
	OUTLET_TYPE("outlet_type"),
	OUTLET_CLASS("outlet_class"),
	OUTLET_CHANNEL("outlet_channel"),
	OUTLET_CATEGORY("outlet_category"),
	OUTLET_SUB_CATEGORY("outlet_sub_channel"),
	LOCATION("location");

	private String sequenceName;

	private Sequence(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public String getSequenceName() {
		return this.sequenceName;
	}

}
