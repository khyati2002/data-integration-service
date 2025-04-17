package com.applicate.ckcoe.Transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutletDetailsCkcoe extends AbstractTransformer<Map<String,Object>,Object> {
    private HierarchyMetadata hmetadata;
    private User userName;
    @Override
    public Object transform(Map<String, Object> inputMap) {

        String outletCode = inputMap.get("outletCode").toString();
        String[] parents = inputMap.get("immediateParent").toString().split(",");

        List<HierarchyMetadata> hierarchyMetadataList = new ArrayList<>();
        List<HierarchyMetadata> hierarchyMetadataListUser = new ArrayList<>();

        for (String parent : parents) {
            HierarchyMetadata hmetadata = new HierarchyMetadata();
            hmetadata.setHierarchy(inputMap.get("outletCode").toString() + ">" + parent);
            hmetadata.setParent(inputMap.get("outletCode").toString());
            hierarchyMetadataList.add(hmetadata);
        }

        for (String parent : parents) {
            HierarchyMetadata hmetadata = new HierarchyMetadata();
            hmetadata.setParent(parent);
            hierarchyMetadataListUser.add(hmetadata);
        }

        userName = new User();
        userName.setLoginId(inputMap.get("userName").toString());
        userName.setImmediateParent(hierarchyMetadataListUser);
        Map<String, Object> outletDetailsTransformer = new HashMap<>();

        outletDetailsTransformer.put("activeStatus", inputMap.get("activeStatus"));
        outletDetailsTransformer.put("outletCode", inputMap.get("outletCode"));
        outletDetailsTransformer.put("userName", userName);
        outletDetailsTransformer.put("outletName", inputMap.get("outletName"));
        outletDetailsTransformer.put("outletType", inputMap.get("outletType"));


        outletDetailsTransformer.put("channel", inputMap.get("channel"));
        outletDetailsTransformer.put("immediateParent", hierarchyMetadataList);

        outletDetailsTransformer.put("latitude",inputMap.get("latitude"));
        outletDetailsTransformer.put("longitude",inputMap.get("longitude"));
        outletDetailsTransformer.put("outletCategory",inputMap.get("outletCategory"));
        outletDetailsTransformer.put("outletClass",inputMap.get("outletClass"));
        outletDetailsTransformer.put("beat",inputMap.get("beat"));
        outletDetailsTransformer.put("beatName",inputMap.get("beatName"));
        outletDetailsTransformer.put("address",inputMap.get("address"));
        outletDetailsTransformer.put("displayAddress",inputMap.get("disAddr"));
        outletDetailsTransformer.put("contactName",inputMap.get("contactName"));
        outletDetailsTransformer.put("contactno",inputMap.get("contactno"));
        outletDetailsTransformer.put("priceListId",inputMap.get("priceListId"));


        Map<String, Object> locationHierarchy = (Map<String, Object>) inputMap.get("locationHierarchy");

        Map<String, Object> location = createLocation(
                (String) locationHierarchy.get("country"),
                (String) locationHierarchy.get("region"),
                (String) locationHierarchy.get("city")
        );

        outletDetailsTransformer.put("location", location);
        return outletDetailsTransformer;
    }

    private static Map<String, Object> createLocation(String country, String region,  String city) {
        Map<String, Object> location = new HashMap<>();
        if (isNotUndefinedOrEmpty(country)) location.put("country", country);
        if (isNotUndefinedOrEmpty(region)) {
            location.put("region", region);
        }
        if (isNotUndefinedOrEmpty(city)) {
            location.put("city", city);
        }
        return location;
    }

    private static boolean isNotUndefinedOrEmpty(String param) {
        return param != null && !param.isEmpty();
    }
}
