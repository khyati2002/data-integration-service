package com.salescode.dataintegration.etl.validation;


import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.PropertyDescriptor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Service
public class FormValidator {


    private final Validator validator;

    public FormValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**
     * Validates the given object and returns a set of constraint violations.
     *
     * @param object The object to validate.
     * @param <T>    The type of the object being validated.
     * @return A set of constraint violations.
     */
    public <T> Set<ConstraintViolation<T>> validate(T object) {
        return validator.validate(object);
    }

    /**
     * Returns a list of constraint properties for the given class.
     *
     * @param clazz The class for which to retrieve constrained properties.
     * @return A list of property names with constraints.
     */
    public List<String> getConstrainedProperties(Class<?> clazz) {
        return validator.getConstraintsForClass(clazz).getConstrainedProperties().stream().map(PropertyDescriptor::getPropertyName).collect(Collectors.toList());
    }

}