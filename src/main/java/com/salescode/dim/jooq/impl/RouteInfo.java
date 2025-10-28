package com.salescode.dim.jooq.impl;


import java.io.Serializable;


public class RouteInfo extends com.salescode.dim.jooq.generated.tables.pojos.RouteInfo implements Serializable {

	private String outletCode;

	private String routeCode;

	private String salesmanCode;

	private String supplier;

	private String salesMode;

	private String routeType;

	private String prodAuthCode;

	public String getOutletCode() {
		return outletCode;
	}

	public void setOutletCode(String outletCode) {
		this.outletCode = outletCode;
	}

	public String getRouteCode() {
		return routeCode;
	}

	public void setRouteCode(String routeCode) {
		this.routeCode = routeCode;
	}

	public String getSalesmanCode() {
		return salesmanCode;
	}

	public void setSalesmanCode(String salesmanCode) {
		this.salesmanCode = salesmanCode;
	}

	public String getSupplier() {
		return supplier;
	}

	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	public String getSalesMode() {
		return salesMode;
	}

	public void setSalesMode(String salesMode) {
		this.salesMode = salesMode;
	}

	public String getRouteType() {
		return routeType;
	}

	public void setRouteType(String routeType) {
		this.routeType = routeType;
	}

	public String getProdAuthCode() {
		return prodAuthCode;
	}

	public void setProdAuthCode(String prodAuthCode) {
		this.prodAuthCode = prodAuthCode;
	}


}
