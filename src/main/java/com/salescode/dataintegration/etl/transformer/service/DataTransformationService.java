package com.salescode.dataintegration.etl.transformer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.AbstractTransformer;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class DataTransformationService {

    private final TransformerInfoRegistry transformerInfoRegistry;
    private final ETLRegistry etlRegistry;
    private final ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    @Autowired
    public DataTransformationService(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry) {
        this.transformerInfoRegistry = transformerInfoRegistry;
        this.etlRegistry = etlRegistry;
    }

    public List<? extends CommonDataModel> transformData(String transformerId, String entityName, Map<String, Object> input) {
        if (transformerId == null || transformerId.isEmpty()) {
            return convertToCommonDataModelList(input, entityName);
        }
        CkTransformerInfo transformerInfo = transformerInfoRegistry.getTransformerInfoById(transformerId);
        AbstractTransformer<Map<String, Object>, Object> transformerInstance = etlRegistry.getTransformer(transformerInfo.getImplementation());
        transformerInstance.setTransformerInfo(transformerInfo);
        Object transformedData = transformerInstance.transform(input);
        return convertToCommonDataModelList(transformedData, entityName);
    }

    public List<? extends CommonDataModel> transformData(String transformerId, String entityName, JsonNode input) {
        Map<String, Object> inputMap = objectMapper.convertValue(input, new TypeReference<>() {});
        return transformData(transformerId, entityName, inputMap);
    }

    private List<? extends CommonDataModel> convertToCommonDataModelList(Object transformedData, String entityName) {
        List<CommonDataModel> result = new ArrayList<>();
        if (transformedData != null) {
            if (transformedData instanceof Collection) {
                ((Collection<?>) transformedData).forEach(item -> result.add(convertToCommonDataModel(item, entityName)));
            } else {
                result.add(convertToCommonDataModel(transformedData, entityName));
            }
            return result;
        }
        throw new RuntimeException("transformedData cannot be null");
    }

    private CommonDataModel convertToCommonDataModel(Object data, String entityName) {
        if (data instanceof CommonDataModel) {
            return (CommonDataModel) data;
        } else {
            Class<? extends CommonDataModel> clazz = EntityUtils.getInstance().getEntityClass(entityName);
            return objectMapper.convertValue(data, clazz);
        }
    }
}