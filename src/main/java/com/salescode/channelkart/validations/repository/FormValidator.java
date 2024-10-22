package com.salescode.channelkart.validations.repository;

import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.PropertyDescriptor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FormValidator implements InitializingBean {

	private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

	@Getter
    private final Validator validator = factory.getValidator();

	private static FormValidator instance;

	public <T> Set<ConstraintViolation<T>> formValidation(T object) {
        return validator.validate(object);
	}

	public List<String> getConstraintsForClass(Class<?> clazz) {

		return validator.getConstraintsForClass(clazz).getConstrainedProperties().stream().map(PropertyDescriptor::getPropertyName)
				.collect(Collectors.toList());
	}

	@Override
	public void afterPropertiesSet(){
		setInstance(this);
	}

	private static synchronized void setInstance(FormValidator f){
		instance = f;
	}

	public static FormValidator get() {
		return instance;
	}

}
