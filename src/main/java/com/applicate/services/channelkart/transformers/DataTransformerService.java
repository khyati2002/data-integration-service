/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.exceptions.UnexpectedResultException;
import com.applicate.services.channelkart.exceptions.checked.ConfigurationException;
import com.applicate.services.channelkart.exceptions.checked.MissingConfigurationException;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.validations.ValidationResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The class DataTransformerService.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */

@Service
public class DataTransformerService<S,T> {

	private Logger logger= LoggerFactory.getLogger(this.getClass());

	/** The default. */
	private static final String DEFAULT="default_";

	/**
	 * Transform.
	 *
	 * @param type the type
	 * @param lob the lob
	 * @param attributeObj the attribute obj
	 * @return the JSON object
	 */
	@SuppressWarnings("unchecked")
	public T transformByType(final String type ,final String lob ,final S attributeObj) {
		if(NullUtils.isNotNull(attributeObj)) {
			List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob,type);
			T resultobj=null;
			if(transformers != null && !transformers.isEmpty()) {
				resultobj = (T) transformData(attributeObj, transformers);
			}
			return resultobj;
		}
		else {
			throw new UnexpectedResultException("attributeObj cannot be null");
		}
	}

	public T transformById(final String id, final String lob ,final S attributeObj) throws ConfigurationException {
		if(NullUtils.isNotNull(attributeObj)) {
			List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob);
			T resultobj=null;
			if(transformers != null && !transformers.isEmpty()) {
				List<TransformerInfo> idtransformer= transformers.stream().filter(transformer-> transformer.getId().equals(id)).collect(Collectors.toList());
				if(idtransformer != null && !idtransformer.isEmpty()) {
					resultobj = (T) transformData(attributeObj, idtransformer);
				}else {
					logger.error("Transformer not found with id : {} for lob : {}",id,lob);
					throw new MissingConfigurationException("Transformer not found with id : {} for lob : {}",id,lob);
				}
			}
			return resultobj;
		}
		else {
			throw new UnexpectedResultException("attributeObj input cannot be null");
		}
	}

	/**
	 * Executes provided transformer on the data
	 *
	 * @param attributeObj the data map
	 * @param idtransformer list of transformers
	 * @return transformed object
	 */
	private Object transformData(final S attributeObj, List<TransformerInfo> idtransformer) {
		try {
			Object result = DataTransformerEngine.INSTANCE.execute(attributeObj, idtransformer.get(0));
			if (NullUtils.isNull(result)) {
				throw new TransformationException(ValidationResponseMessage.NULL_TRANSFORMATION_RESULT);
			}
			JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
			return handleResult(result);
		} catch (TransformationException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Exception happened while transforming the data:{} using TransformerInfo:{}", attributeObj, idtransformer);
			logger.error("stackTrace:", e);
			throw new TransformationException(ValidationResponseMessage.NULL_TRANSFORMATION_RESULT);
		}
	}

	/**
	 * Validates and return the transformed record based on JSON Schema.
	 * @param idtransformer TransformerInfo Records based on transformerID
	 * @param factory JsonSchema Factory to read and validate using JSON Schema
	 * @param result Transformed Result
	 * @return <b>result</b>
	 */
//	private Object validateAndTransform(List<TransformerInfo> idtransformer, JsonSchemaFactory factory, Object result) {
//		JsonNode schemaNode = idtransformer.get(0).getJsonSchema();
//		if (isSchemaPresent(schemaNode)) {
//			JsonSchema jsonSchema = factory.getSchema(schemaNode);
//			if (result instanceof Collection) {
//				return validateAndTransformResult(jsonSchema, (Collection<?>) result);
//			} else {
//				return validateAndTransformResult(jsonSchema, List.of(result));
//			}
//		} else {
//			return handleResult(result);
//		}
//	}

	/**
	 * Validates transformedResult based on the JSON Schema and returns transformedResult
	 * @param jsonSchema {@linkplain TransformerInfo.jsonSchema }
	 * @param result Collection of transformedResult
	 * @return result
	 */
	private Collection<?> validateAndTransformResult(JsonSchema jsonSchema, Collection<?> result) {
		List<ValidationMessage> messages = result.stream().flatMap(res -> jsonSchema.validate(JSONUtils.toJsonNode(res)).stream()).collect(Collectors.toList());
		if (messages.isEmpty()) {
			return result;
		} else {
			throw new TransformationException(messages.stream().map(ValidationMessage::getMessage).collect(Collectors.joining(", ")));
		}
	}

	/**
	 * Method to return transformedResult in case not JSON Schema validation is required.
	 * @param result transformed result
	 * @return Collection of transformedResult
	 */
	private Object handleResult(Object result) {
		if (!(result instanceof Collection)) {
			return List.of(result);
		} else {
			return result;
		}
	}

	/**
	 * Method to check whether JSON Schema exists for the transformer or not
	 * @param schema jsonSchema from {@link TransformerInfo}
	 * @return boolean existence of schema
	 */
	private boolean isSchemaPresent(JsonNode schema) {
		return NullUtils.isNotNull(schema) && !schema.isEmpty();
	}

	public T transform(final String id ,final String type, final String lob ,final S attributeObj) throws ConfigurationException {
		if(id != null) {
			return transformById(id,lob,attributeObj);
		}else {
			return transformById(DEFAULT+type.toLowerCase(),lob,attributeObj);
		}
	}

	public TransformerInfo getTransformerById(String lob, String transformerId) {
		List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob);
		if(transformers != null && !transformers.isEmpty()) {
			return transformers.stream().filter(transformer-> transformer.getId().equals(transformerId)).findFirst().orElse(null);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object transformByType(final String type ,final String lob ,final S attributeObj,final boolean preserveReturnType) {
		if(NullUtils.isNotNull(attributeObj)) {
			List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob,type);
			Object resultobj=null;
			if(transformers != null && !transformers.isEmpty()) {
				Object result= DataTransformerEngine.INSTANCE.execute(attributeObj,transformers.get(0));
				resultobj=  result;
			}
			return resultobj;
		}
		else {
			throw new UnexpectedResultException("attributeObj cannot be null");
		}
	}

	@SuppressWarnings("unchecked")
	public Object transformById(final String id, final String lob ,final S attributeObj,final boolean preserveReturnType) throws ConfigurationException {
		if(NullUtils.isNotNull(attributeObj)) {
			List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob);
			Object resultobj=null;
			if(transformers != null && !transformers.isEmpty()) {
				List<TransformerInfo> idtransformer= transformers.stream().filter(transformer-> transformer.getId().equals(id)).collect(Collectors.toList());
				if(idtransformer != null && !idtransformer.isEmpty()) {
					Object result= DataTransformerEngine.INSTANCE.execute(attributeObj,idtransformer.get(0));
					resultobj=  result;
				}else {
					logger.error("Transformer not found with id : {} for lob : {}",id,lob);
					throw new MissingConfigurationException("Transformer not found with id : {} for lob : {}",id,lob);
				}
			}
			return resultobj;
		}
		else {
			throw new UnexpectedResultException("attributeObj input cannot be null");
		}
	}

	public Object transform(final String id ,final String type, final String lob ,final S attributeObj,final boolean preserveReturnType) throws ConfigurationException {
		if(id != null) {
			return transformById(id,lob,attributeObj,preserveReturnType);
		}else {
			return transformById(DEFAULT+type.toLowerCase(),lob,attributeObj,preserveReturnType);
		}
	}
	
	/**
	 * Transform by name.
	 *
	 * @param name the name
	 * @param lob the lob
	 * @param inputObj the input obj
	 * @return the object
	 */
	@SuppressWarnings("unchecked")
	public T transformByName(final String name ,final String lob ,final S inputObj) {
		if(NullUtils.isNotNull(inputObj)) {
			Optional<TransformerInfo> transformer = TransformerRegistry.INSTANCE.getByName(lob,name);
			if(transformer.isPresent()) {
				return (T) transformData(inputObj, List.of(transformer.get()));
			}
			return null;
		}
		else {
			throw new UnexpectedResultException("attributeObj cannot be null");
		}
	}

	public boolean isTransformerExists(final String id, final String lob) {
		List<TransformerInfo> transformers = TransformerRegistry.INSTANCE.get(lob);
		if (transformers != null && !transformers.isEmpty()) {
			List<TransformerInfo> idtransformer = transformers.stream().filter(transformer -> transformer.getId().equals(id)).collect(Collectors.toList());
			if (idtransformer != null && !idtransformer.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
}