package com.salescode.dim.etl.transformation.service;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.*;

/**
 * A lightweight data transformation service that:
 */
public class DataTransformationService implements Serializable {

    private static final long serialVersionUID = -569164891930010577L;

    private final transient TransformerInfoRegistry transformerInfoRegistry;
    private final transient ETLRegistry etlRegistry;
    private final transient EntityUtils entityUtils;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with required dependencies.
     */
    public DataTransformationService(TransformerInfoRegistry transformerInfoRegistry, ETLRegistry etlRegistry, EntityUtils entityUtils) {
        this.transformerInfoRegistry = Objects.requireNonNull(transformerInfoRegistry, "transformerInfoRegistry must not be null");
        this.etlRegistry = Objects.requireNonNull(etlRegistry, "etlRegistry must not be null");
        this.entityUtils = Objects.requireNonNull(entityUtils, "entityUtils must not be null");
        this.objectMapper = JSONUtils.getObjectMapper();
    }

    /**
     * Transforms the given input data using a specified transformer.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityClass   the target entity class.
     * @param input         the input data as a map.
     * @return a list of CommonDataModel objects representing the transformed data.
     * @throws IllegalArgumentException if required parameters are invalid
     * @throws TransformationException  if transformation fails
     */
    public List<CommonDataModel> transformData(String transformerId, Class<? extends CommonDataModel> entityClass, Map<String, Object> input) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(input, "input must not be null");

        if (transformerId == null || transformerId.isEmpty()) {
            return convertToCommonDataModelList(input, entityClass);
        }

        try {
            Object transformedData = applyTransformer(transformerId, input);
            return convertToCommonDataModelList(transformedData, entityClass);
        } catch (Exception e) {
            throw new TransformationException("Failed to transform data with transformer ID: " + transformerId, e);
        }
    }

    /**
     * Transforms the given input data using a specified transformer.
     * This overloaded version accepts an entity name instead of a class object.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityName    the name of the target entity.
     * @param input         the input data as a map.
     * @return a list of CommonDataModel objects representing the transformed data.
     * @throws IllegalArgumentException if required parameters are invalid or entity not found
     * @throws TransformationException  if transformation fails
     */
    @SuppressWarnings("unchecked")
    public List<CommonDataModel> transformData(String transformerId, String entityName, Map<String, Object> input) {
        Objects.requireNonNull(entityName, "entityName must not be null");
        Objects.requireNonNull(input, "input must not be null");

        try {
            // Resolve the entity class from the entity name
            Class<? extends CommonDataModel> entityClass = entityUtils.getEntityClass(entityName);
            if (entityClass == null) {
                throw new IllegalArgumentException("Entity class not found for entity name: " + entityName);
            }

            // Delegate to the class-based implementation
            return transformData(transformerId, entityClass, input);
        } catch (ClassCastException e) {
            throw new TransformationException("Entity class for name '" + entityName + "' is not a CommonDataModel", e);
        } catch (Exception e) {
            if (e instanceof TransformationException) {
                throw e;
            }
            throw new TransformationException("Failed to transform data for entity name: " + entityName, e);
        }
    }

    /**
     * Transforms the given input data using a specified transformer.
     * This overloaded version accepts an entity name instead of a class object.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityName    the name of the target entity.
     * @param input         the input data as a JsonNode.
     * @return a list of CommonDataModel objects representing the transformed data.
     * @throws IllegalArgumentException if required parameters are invalid or entity not found
     * @throws TransformationException  if transformation fails
     */
    public List<CommonDataModel> transformData(String transformerId, String entityName, JsonNode input) {
        Objects.requireNonNull(entityName, "entityName must not be null");
        Objects.requireNonNull(input, "input must not be null");

        Map<String, Object> inputMap = objectMapper.convertValue(input, new TypeReference<Map<String, Object>>() {
        });
        return transformData(transformerId, entityName, inputMap);
    }

    /**
     * Transforms the given input data using a specified transformer.
     *
     * @param transformerId the ID of the transformer; if null or empty, no transformer is applied.
     * @param entityClass   the target entity class.
     * @param input         the input data as a JsonNode.
     * @return a list of CommonDataModel objects representing the transformed data.
     * @throws IllegalArgumentException if required parameters are invalid
     * @throws TransformationException  if transformation fails
     */
    public List<CommonDataModel> transformData(String transformerId, Class<? extends CommonDataModel> entityClass, JsonNode input) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(input, "input must not be null");

        Map<String, Object> inputMap = objectMapper.convertValue(input, new TypeReference<Map<String, Object>>() {
        });
        return transformData(transformerId, entityClass, inputMap);
    }

    /**
     * Applies the specified transformer to the input data.
     *
     * @param transformerId the ID of the transformer
     * @param input         the input data
     * @return the transformed data
     * @throws IllegalArgumentException if transformer not found
     */
    private Object applyTransformer(String transformerId, Map<String, Object> input) {
        TransformerInfo transformerInfo = Optional.ofNullable(transformerInfoRegistry.getTransformerInfoById(transformerId))
                                                  .orElseThrow(() -> new IllegalArgumentException("Transformer not found with ID: " + transformerId));

        AbstractTransformer<Map<String, Object>, Object> transformerInstance = etlRegistry.getTransformer(transformerInfo.getImplementation());

        if (transformerInstance == null) {
            throw new IllegalArgumentException("Transformer implementation not found: " + transformerInfo.getImplementation());
        }

        transformerInstance.setTransformerInfo(transformerInfo);
        return transformerInstance.transform(input);
    }

    /**
     * Converts transformed data into a list of CommonDataModel instances.
     *
     * @param transformedData the data to convert
     * @param entityClass     the target entity class
     * @return a list of converted entities
     * @throws TransformationException if conversion fails
     */
    private List<CommonDataModel> convertToCommonDataModelList(Object transformedData, Class<? extends CommonDataModel> entityClass) {
        if (transformedData == null) {
            return Collections.emptyList();
        }

        List<CommonDataModel> result = new ArrayList<>();
        try {
            if (transformedData instanceof Collection) {
                for (Object item : (Collection<?>) transformedData) {
                    result.add(convertToCommonDataModel(item, entityClass));
                }
            } else {
                result.add(convertToCommonDataModel(transformedData, entityClass));
            }
            return result;
        } catch (Exception e) {
            throw new TransformationException("Failed to convert transformed data to " + entityClass.getName(), e);
        }
    }

    /**
     * Converts an individual data object into a CommonDataModel instance.
     *
     * @param data        the data to convert
     * @param entityClass the target entity class
     * @return the converted entity
     */
    @SuppressWarnings("unchecked")
    private <T extends CommonDataModel> T convertToCommonDataModel(Object data, Class<T> entityClass) {
        if (data == null) {
            throw new IllegalArgumentException("Data to convert cannot be null");
        }

        // If data is already the correct type, just cast it
        if (data instanceof CommonDataModel) {
            if (entityClass.isInstance(data)) {
                return entityClass.cast(data);
            }
            throw new IllegalArgumentException("Data is not an instance of " + entityClass.getName());
        }

        // Otherwise convert using object mapper
        return objectMapper.convertValue(data, entityClass);
    }

    /**
     * Exception thrown when a transformation operation fails.
     */
    public static class TransformationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TransformationException(String message) {
            super(message);
        }

        public TransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}