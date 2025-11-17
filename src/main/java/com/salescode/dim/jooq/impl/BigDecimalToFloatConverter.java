package com.salescode.dim.jooq;

import org.jooq.Converter;
import java.math.BigDecimal;

public class BigDecimalToFloatConverter implements Converter<BigDecimal, Float> {

	@Override
	public Float from(BigDecimal databaseObject) {
		return databaseObject == null ? null : databaseObject.floatValue();
	}

	@Override
	public BigDecimal to(Float userObject) {
		return userObject == null ? null : BigDecimal.valueOf(userObject);
	}

	@Override
	public Class<BigDecimal> fromType() {
		return BigDecimal.class;
	}

	@Override
	public Class<Float> toType() {
		return Float.class;
	}
}
