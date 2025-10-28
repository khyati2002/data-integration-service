package com.applicate.services.channelkart.services;


import com.salescode.dim.jooq.impl.GRNInfo;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Data
public class SalesGrnService extends AbstractCDMService<GRNInfo> {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(SalesGrnService.class);






  /**
   * Updates float fields in the SalesDetailsDTO object.
   *
   * @param dto The DTO object to update
   * @param sd  The object containing the new data
   */
  public <T> void updateFloatFields(T dto, T sd) {
    Stream.of(
        "CaseQuantity", "PieceQuantity", "OtherUnitQuantity",
        "InitialCaseQuantity", "InitialPieceQuantity", "InitialOtherUnitQuantity",
        "InitialNormalizedQuantity", "NormalizedQuantity")
        .forEach(field -> updateField(dto, sd, field, this::getFloatValueSafely, float.class));
  }

  /**
   * Updates double fields in the SalesDetailsDTO object.
   *
   * @param dto The DTO object to update
   * @param sd  The object containing the new data
   */
  public <T> void updateDoubleFields(T dto, T sd) {
    Stream.of("BillAmount", "NetAmount", "InitialAmount")
        .forEach(field -> updateField(dto, sd, field, this::getDoubleValueSafely, double.class));
  }

  /**
   * Updates a single field in a DTO object using reflection.
   *
   * @param <T>         The type of the field (must extend Number)
   * @param <D>         The type of the DTO object
   * @param <S>         The type of the source object
   * @param dto         The DTO object to update
   * @param source      The source object containing the new data
   * @param fieldName   The name of the field to update
   * @param valueGetter A function to safely get the value from a Number object
   * @param fieldType   The Class object representing the type of the field
   */
  @SuppressWarnings("unchecked")
  public <T extends Number, D, S> void updateField(D dto, S source, String fieldName,
      Function<Number, T> valueGetter, Class<T> fieldType) {
    try {
      String setterName = "set" + fieldName;
      String getterName = "get" + fieldName;

      Method dtoSetter = dto.getClass().getMethod(setterName, fieldType);
      Method dtoGetter = dto.getClass().getMethod(getterName);
      Method sourceGetter = source.getClass().getMethod(getterName);

      T dtoValue = valueGetter.apply((Number) dtoGetter.invoke(dto));
      T sourceValue = valueGetter.apply((Number) sourceGetter.invoke(source));

      dtoSetter.invoke(dto, add(dtoValue, sourceValue));
    } catch (Exception e) {
      logger.error("STACKTRACE",e);
    }
  }

  /**
   * Adds two Number objects of the same type.
   *
   * @param <T> The type of the numbers (must extend Number)
   * @param a   The first number
   * @param b   The second number
   * @return The sum of the two numbers
   * @throws IllegalArgumentException if the type is not supported
   */
  @SuppressWarnings({ "unchecked" })
  private <T extends Number> T add(T a, T b) {
    if (a instanceof Float) {
      return (T) Float.valueOf(a.floatValue() + b.floatValue());
    } else if (a instanceof Double) {
      return (T) Double.valueOf(a.doubleValue() + b.doubleValue());
    }
    throw new IllegalArgumentException("Unsupported type for addition: " + a.getClass().getSimpleName());
  }

  /**
   * Safely retrieves a Float value from a Number object.
   *
   * @param value The Number object to get the value from
   * @return The float value, or 0f if the input is null
   */
  public Float getFloatValueSafely(Number value) {
    return (value != null) ? value.floatValue() : 0f;
  }

  /**
   * Safely retrieves a Double value from a Number object.
   *
   * @param value The Number object to get the value from
   * @return The double value, or 0.0 if the input is null
   */
  public Double getDoubleValueSafely(Number value) {
    return (value != null) ? value.doubleValue() : 0.0;
  }

}
