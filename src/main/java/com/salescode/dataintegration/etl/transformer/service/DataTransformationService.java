package com.salescode.dataintegration.etl.transformer.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.transformers.TransformerInfo;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.AbstractTransformer;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class DataTransformationService {

    private final TransformerInfoRegistry transformerInfoRegistry;
    private final ETLRegistry etlRegistry;
    private final ObjectMapper objectMapper;

    @Autowired
    public DataTransformationService(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry) {
        this.transformerInfoRegistry = transformerInfoRegistry;
        this.etlRegistry = etlRegistry;
        this.objectMapper = new ObjectMapper();
        JacksonAnnotationIntrospector ignoreEnumAliasAnnotations = new JacksonAnnotationIntrospector() {
            private static final long serialVersionUID = -3342803373202589544L;

            @Override
            protected <A extends Annotation> A _findAnnotation(Annotated annotated, Class<A> annoClass) {
                if (annotated.hasAnnotation(JsonFormat.class)) {
                    return super._findAnnotation(annotated, annoClass);
                }
                if (annotated.hasAnnotation(JsonDeserialize.class)) {
                    JsonDeserialize jsonDeserializer = annotated.getAnnotation(JsonDeserialize.class);
                    if (jsonDeserializer.using() != JsonDeserializer.None.class) {
                        return super._findAnnotation(annotated, annoClass);
                    }
                }
                return null;
            }
        };
        objectMapper.setAnnotationIntrospector(ignoreEnumAliasAnnotations);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    public List<? extends CommonDataModel> transformData(String transformerId, String entityName, Map<String, Object> input) {
        if (transformerId == null || transformerId.isEmpty()) {
            return convertToCommonDataModelList(input, entityName);
        }
        TransformerInfo transformerInfo = transformerInfoRegistry.getTransformerInfoById(transformerId);
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
            Class<? extends CommonDataModel> clazz = EntityUtils.get().getEntityClass(entityName);
            return objectMapper.convertValue(data, clazz);
        }
    }
}