package com.salescode.dataintegration.bundle;

import com.salescode.dataintegration.etl.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TestTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final Logger log = LoggerFactory.getLogger(TestTransformer.class);

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> stringObjectMap) {
        log.info("Transforming data inside bundle");
        return List.of(stringObjectMap);
    }
}
