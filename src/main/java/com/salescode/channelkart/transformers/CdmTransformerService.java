/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.transformers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.salescode.channelkart.component.model.AbstractExecutor;
import com.salescode.channelkart.exceptions.TransformationException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.channelkart.validations.ValidationResponseMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The class CdmTransformerService.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
@Service
@Qualifier("CdmTransformerService")
public class CdmTransformerService implements AbstractExecutor<List<CommonDataModel>, Map<String, Object>> {

    /**
     * The entity utils.
     */
    private final ObjectMapper mapper;

    /**
     * Instantiates a new cdm transformer executor service.
     */
    public CdmTransformerService() {

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

        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(ignoreEnumAliasAnnotations);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }


    /**
     * Execute.
     *
     * @param objects the objects
     * @return the list
     */

    @SuppressWarnings("unchecked")
    @Override
    public List<CommonDataModel> execute(Map<String, Object> element, Object... objects) {
        List<CommonDataModel> data = new ArrayList<>();
        try {
            String entityName = (String) objects[0];
            String lob = (String) objects[1];
            String id = (String) objects[2];
            Class<?> clazz = EntityUtils.get().getEntityClass(entityName);
            element.put("lob", lob);
            @SuppressWarnings("rawtypes") List transformResult = new DataTransformerService<Map<String, Object>, List<Object>>()
                    .transform(id, entityName, lob, element);
            if (transformResult != null && !transformResult.isEmpty()) {
                if (transformResult.get(0) instanceof CommonDataModel) {
                    data.addAll(transformResult);
                } else {
                    transformResult.forEach(transformedelement -> data.add((CommonDataModel) mapper.convertValue(transformedelement, clazz)));
                }
            } else {
                throw new TransformationException(ValidationResponseMessage.NULL_TRANSFORMATION_RESULT);
            }
        } catch (Exception ex) {
            Throwable throwable = ExceptionUtils.getRootCause(ex);
            if (throwable instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
                String message = throwable.getLocalizedMessage();
                String[] msg = message.split(":");
                String localmsg = msg[0];
                String mop = msg[5];
                localmsg += " for " + mop.substring(mop.lastIndexOf(".") + 1, mop.length() - 1);
                throw new TransformationException(ex, StringUtils.format(ValidationResponseMessage.INVALID_INPUT_FOR_TRANSFORMER, localmsg));
            } else {
                throw new TransformationException(ex, ExceptionUtils.getRootCauseMessage(ex));
            }
        }
        return data;
    }

    public List<CommonDataModel> toCDM(Map<String, Object> element, String entityName) {
        Class<?> clazz = EntityUtils.get().getEntityClass(entityName);
        return Arrays.asList((CommonDataModel) mapper.convertValue(element, clazz));
    }

}
