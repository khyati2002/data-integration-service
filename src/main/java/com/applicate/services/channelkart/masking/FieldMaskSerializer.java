package com.applicate.services.channelkart.masking;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class FieldMaskSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private final PropertyRegistry propertyRegistry;

    private final MaskField maskField;

    @Autowired
    public FieldMaskSerializer(PropertyRegistry propertyRegistry) {
        this(propertyRegistry, null);
    }

    public FieldMaskSerializer() {
        this(null, null);
    }

    private FieldMaskSerializer(PropertyRegistry propertyRegistry, MaskField maskFieldAnnotation) {
        super(Object.class);
        this.propertyRegistry = propertyRegistry;
        this.maskField = maskFieldAnnotation;
    }

    public MaskField getMaskField() {
        return maskField;
    }

    @Override
    public FieldMaskSerializer createContextual(SerializerProvider prov, com.fasterxml.jackson.databind.BeanProperty property) {
        if (property != null) {
            MaskField annotation = property.getAnnotation(MaskField.class);
            if (annotation != null) {
                return new FieldMaskSerializer(propertyRegistry, annotation);
            }

        }
        return this;
    }


    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null && propertyRegistry != null && propertyRegistry.getAsBoolean(PropertyDefinition.SECURITY_STRICT_ACCESS_CHECK_ENABLED)) {
            gen.writeString("*******");
        } else if (value != null) {
            gen.writeString(value.toString());
        }
    }
}