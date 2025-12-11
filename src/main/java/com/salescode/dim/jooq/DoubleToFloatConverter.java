package com.salescode.dim.jooq;

import org.jooq.Converter;

public class DoubleToFloatConverter implements Converter<Double, Float> {

    @Override
    public Float from(Double databaseObject) {
        return databaseObject == null ? null : databaseObject.floatValue();
    }

    @Override
    public Double to(Float userObject) {
        return userObject == null ? null : userObject.doubleValue();
    }

    @Override
    public Class<Double> fromType() {
        return Double.class;
    }

    @Override
    public Class<Float> toType() {
        return Float.class;
    }
}