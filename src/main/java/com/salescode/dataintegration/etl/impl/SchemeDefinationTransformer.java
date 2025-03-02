package com.salescode.dataintegration.etl.impl;

import com.salescode.dataintegration.etl.transformer.AbstractTransformer;

import java.util.List;
import java.util.Map;

public class SchemeDefinationTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> stringObjectMap) {
        System.out.println("Transforming: " + stringObjectMap);
        return List.of(stringObjectMap);
    }
}
