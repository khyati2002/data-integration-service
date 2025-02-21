package com.salescode.dim.transformers.service;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import com.salescode.dim.registry.ETLRegistry;
import com.salescode.dim.transformers.AbstractTransformer;
import com.salescode.dim.transformers.registry.TransformerInfoRegistry;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A lightweight data transformation service that:
 * <ul>
 *     <li>Removes Spring dependencies.</li>
 *     <li>Simplifies the overall design and reduces complexity.</li>
 *     <li>Uses direct loops for clarity and performance.</li>
 * </ul>
 * <p>
 * This class is implemented as a singleton.
 */
public class DataTransformationService implements Serializable {

    private static final long serialVersionUID = -569164891930010577L;

    private static DataTransformationService instance;

    private final transient TransformerInfoRegistry transformerInfoRegistry;
    private final transient ETLRegistry etlRegistry;
    private final transient EntityUtils entityUtils;
    private final ObjectMapper objectMapper;

    /**
     * Private constructor to prevent instantiation.
     */
    private DataTransformationService(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry, EntityUtils entityUtils) {
        this.transformerInfoRegistry = transformerInfoRegistry;
        this.etlRegistry = etlRegistry;
        this.entityUtils = entityUtils;
        this.objectMapper = JSONUtils.getObjectMapper();
    }

    /**
     * Provides a thread-safe singleton instance of DataTransformationService.
     *
     * @param transformerInfoRegistry the TransformerInfoRegistry dependency.
     * @param etlRegistry             the ETLRegistry dependency.
     * @return the singleton instance.
     */
    public static DataTransformationService getInstance(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry, EntityUtils entityUtils) {
        if (instance == null) {
            instance = new DataTransformationService(transformerInfoRegistry, etlRegistry, entityUtils);
        }
        return instance;
    }

    /**
     * Ensures that the singleton instance is preserved during deserialization.
     *
     * @return the singleton instance.
     * @throws ObjectStreamException if an error occurs.
     */
    private Object readResolve() throws ObjectStreamException {
        return instance;
    }

    /**
     * Transforms the given input data using a specified transformer.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityName    the target entity name.
     * @param input         the input data as a map.
     * @return a list of CommonDataModel objects representing the transformed data.
     */
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

    /**
     * Transforms the given input data using a specified transformer.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityName    the target entity name.
     * @param input         the input data as a JsonNode.
     * @return a list of CommonDataModel objects representing the transformed data.
     */
    public List<? extends CommonDataModel> transformData(String transformerId, String entityName, JsonNode input) {
        Map<String, Object> inputMap = objectMapper.convertValue(input, new TypeReference<Map<String, Object>>() {
        });
        return transformData(transformerId, entityName, inputMap);
    }

    /**
     * Converts transformed data into a list of CommonDataModel instances.
     *
     * @param transformedData the data to be converted.
     * @param entityName      the target entity name.
     * @return a list of CommonDataModel instances.
     * @throws RuntimeException if the transformed data is null.
     */
    private List<? extends CommonDataModel> convertToCommonDataModelList(Object transformedData, String entityName) {
        if (transformedData == null) {
            throw new RuntimeException("transformedData cannot be null");
        }
        List<CommonDataModel> result = new ArrayList<>();
        if (transformedData instanceof Collection) {
            for (Object item : (Collection<?>) transformedData) {
                result.add(convertToCommonDataModel(item, entityName));
            }
        } else {
            result.add(convertToCommonDataModel(transformedData, entityName));
        }
        return result;
    }

    /**
     * Converts an individual data object into a CommonDataModel instance.
     *
     * @param data       the data object to convert.
     * @param entityName the target entity name.
     * @return a CommonDataModel instance.
     */
    private CommonDataModel convertToCommonDataModel(Object data, String entityName) {
        if (data instanceof CommonDataModel) {
            return (CommonDataModel) data;
        }
        Class<? extends CommonDataModel> clazz = entityUtils.getEntityClass(entityName);
        return objectMapper.convertValue(data, clazz);
    }
}