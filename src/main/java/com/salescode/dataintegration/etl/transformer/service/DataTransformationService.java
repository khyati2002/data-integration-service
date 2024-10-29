package com.salescode.dataintegration.etl.transformer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.Transformer;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DataTransformationService(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry) {
        this.transformerInfoRegistry = transformerInfoRegistry;
        this.etlRegistry = etlRegistry;
    }

    public List<CommonDataModel> transformData(String transformerId, String entityName, Map<String, Object> input) {
        CkTransformerInfo transformerInfo = transformerInfoRegistry.getTransformerInfoById(transformerId);
        Transformer<Map<String, Object>, Object> transformerInstance = etlRegistry.getTransformer(transformerInfo.getImplementation());
        Object transformedData = transformerInstance.transform(input);
        return convertToCommonDataModelList(transformedData, entityName);
    }

    public List<CommonDataModel> transformData(String transformerId, String entityName, JsonNode input) {
        Map<String, Object> inputMap = objectMapper.convertValue(input, new TypeReference<>() {});
        return transformData(transformerId, entityName, inputMap);
    }

    private List<CommonDataModel> convertToCommonDataModelList(Object transformedData, String entityName) {
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
            Class<? extends CommonDataModel> clazz = EntityUtils.getEntityClass(entityName);
            return objectMapper.convertValue(data, clazz);
        }
    }
}