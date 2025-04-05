package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class StreamingRawDataCdmCollector {

    Map<Class<? extends CommonDataModel>, Collection<CommonDataModel>> dataset = new LinkedHashMap<>();

    void collect(Class<? extends CommonDataModel> key, Collection<CommonDataModel> value) {
        dataset.computeIfAbsent(key, k -> new HashSet<>()).addAll(value);
    }

    public Collection<Class<? extends CommonDataModel>> getClasses() {
        return dataset.keySet();
    }

}
