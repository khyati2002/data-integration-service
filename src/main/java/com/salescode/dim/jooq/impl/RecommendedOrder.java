package com.salescode.dim.jooq.impl;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

public class RecommendedOrder extends com.salescode.dim.jooq.generated.tables.pojos.RecommendedOrder implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;

    public RecommendedOrder() {
        super();
    }

    private RecommendedOrder(com.salescode.dim.jooq.generated.tables.pojos.RecommendedOrder recommendedOrder) {
        super(recommendedOrder);
    }

    public static RecommendedOrder of(com.salescode.dim.jooq.generated.tables.pojos.RecommendedOrder recommendedOrder) {
        if (recommendedOrder == null) {
            return null;
        }
        return new RecommendedOrder(recommendedOrder);
    }

    public String getSupportKPI() {
        return getSupportkpi();
    }

    @JsonSetter("supportKPI")
    public void setSupportKPI(String supportKPI) {
        super.setSupportkpi(supportKPI);
    }

    public String getOutletCode() {
        return getOutletcode();
    }

    @JsonSetter("outletCode")
    public void setOutletCode(String outletCode) {
        super.setOutletcode(outletCode);
    }

    public String getLoginId() {
        return getLoginid();
    }

    @JsonSetter("loginId")
    public void setLoginId(String loginId) {
        setLoginid(loginId);
    }


}
