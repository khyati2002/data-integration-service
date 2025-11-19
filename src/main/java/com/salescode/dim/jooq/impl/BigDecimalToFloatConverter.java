package com.salescode.dim.jooq;

import org.jooq.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalToFloatConverter implements Converter<BigDecimal, Float> {

	@Override
	public Float from(BigDecimal databaseObject) {
		if (databaseObject == null) {
			return null;
		}

		// keep DB precision but convert to float (will naturally truncate)
		return databaseObject.floatValue(); // results in 2322.6047
	}

	@Override
	public BigDecimal to(Float userObject) {
		if (userObject == null) {
			return null;
		}

		// Convert float -> BigDecimal from exact string representation
		BigDecimal bd = new BigDecimal(Float.toString(userObject));

		// force to 8 decimal places to match DB
		return bd.setScale(8, RoundingMode.HALF_UP); // 2322.60470000
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
