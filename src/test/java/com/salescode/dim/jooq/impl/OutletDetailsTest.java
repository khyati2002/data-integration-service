package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.SneakyThrows;
import org.junit.Test;

public class OutletDetailsTest {

    String s = "{\n" +
            "  \"userName\" : {\n" +
            "    \"locationHierarchy\" : {\n" +
            "      \"country\" : \"India\",\n" +
            "      \"branch\" : \"EVIZ\",\n" +
            "      \"district\" : \"EDIS\"\n" +
            "    },\n" +
            "    \"activeStatus\" : \"active\",\n" +
            "    \"activeStatusReason\" : \"active\",\n" +
            "    \"designation\" : [ \"retailer\" ],\n" +
            "    \"contactType\" : \"retailer\",\n" +
            "    \"loginId\" : \"C20220005809717\",\n" +
            "    \"userAccountId\" : \"C20220005809717\",\n" +
            "    \"extendedAttributes\" : {\n" +
            "      \"loyaltyFlag\" : \"non loyalty\"\n" +
            "    },\n" +
            "    \"dob\" : null,\n" +
            "    \"name\" : \"VISHAKA PALOUR\",\n" +
            "    \"doa\" : null,\n" +
            "    \"immediateParent\" : [ {\n" +
            "      \"immediateParent\" : \"VI3493\"\n" +
            "    } ]\n" +
            "  },\n" +
            "  \"location\" : {\n" +
            "    \"country\" : \"India\",\n" +
            "    \"branch\" : \"EVIZ\",\n" +
            "    \"district\" : \"EDIS\"\n" +
            "  },\n" +
            "  \"activeStatus\" : \"active\",\n" +
            "  \"activeStatusReason\" : \"active\",\n" +
            "  \"outletCode\" : \"C20220005809717\",\n" +
            "  \"outletCategory\" : \"non loyalty\",\n" +
            "  \"extendedAttributes\" : {\n" +
            "    \"PCPTier\" : null,\n" +
            "    \"custOrder\" : \"Y\",\n" +
            "    \"foodsTier\" : null,\n" +
            "    \"PCPSubType\" : null,\n" +
            "    \"custLoyalty\" : \"N\",\n" +
            "    \"giftVoucher\" : \"Y\",\n" +
            "    \"ITCProducts\" : \"Y\",\n" +
            "    \"autoRedemption\" : \"Y\",\n" +
            "    \"FCFoodsSubType\" : null,\n" +
            "    \"supplierMapping\" : [ {\n" +
            "      \"UID\" : \"C20220005809717\",\n" +
            "      \"RCSId\" : \"181204899725\",\n" +
            "      \"CustID\" : \"UK029\",\n" +
            "      \"SIFYID\" : \"VI3493CIS722UK029\",\n" +
            "      \"WDDest\" : \"VI3493\",\n" +
            "      \"WDName\" : \"SRI DEVAKI LOGISTICS\",\n" +
            "      \"CatMapping\" : \"\"\n" +
            "    } ]\n" +
            "  },\n" +
            "  \"outletName\" : \"VISHAKA PALOUR\",\n" +
            "  \"latitude\" : null,\n" +
            "  \"contactName\" : \"VISHAKA PALOUR\",\n" +
            "  \"longitude\" : null,\n" +
            "  \"outletType\" : \"Convenience Outlet\",\n" +
            "  \"doo\" : null,\n" +
            "  \"channel\" : \"Retail\",\n" +
            "  \"address\" : \"KARANAM GARI JN\",\n" +
            "  \"displayAddress\" : \"KARANAM GARI JN\",\n" +
            "  \"outletClass\" : \"Retail Others\",\n" +
            "  \"immediateParent\" : [ {\n" +
            "    \"immediateParent\" : \"C20220005809717\",\n" +
            "    \"hierarchy\" : \"C20220005809717' > 'VI3493\"\n" +
            "  } ]\n" +
            "}";

    @SneakyThrows
    @Test
    public void test() {
        OutletDetails details = JSONUtils.getObjectMapper().readValue(s, OutletDetails.class);
        System.out.println(details);
    }

}