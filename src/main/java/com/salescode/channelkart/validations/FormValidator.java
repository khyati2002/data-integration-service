package com.salescode.channelkart.validations;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FormValidator implements InitializingBean {

	private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

	private Validator validator = factory.getValidator();

	private static FormValidator instance;

	public <T> Set<ConstraintViolation<T>> formValidation(T object) {
		Set<ConstraintViolation<T>> constraintViolations = validator.validate(object);
		return constraintViolations;
	}

	public List<String> getConstraintsForClass(Class<?> clazz) {

		return validator.getConstraintsForClass(clazz).getConstrainedProperties().stream().map(p -> p.getPropertyName())
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

	public Validator getValidator() {
		return validator;
	}

}
