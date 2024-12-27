/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.transformers;


import com.salescode.channelkart.utils.ReflectionUtils;

/**
 * The class DataTransformerEngine.
 *
 * @param <S> the generic type
 * @param <T> the generic type
 * @author Manish Srivastava
 * @since May 2020
 */
public class DataTransformerEngine<S, T> {

	/**
	 * The Constant INSTANCE.
	 */
	@SuppressWarnings("rawtypes")
	public static final DataTransformerEngine INSTANCE = new DataTransformerEngine();

	/**
	 * Execute.
	 *
	 * @param inputObj        the input obj
	 * @param transformerInfo the transformer info
	 * @return the t
	 */
	public T execute(S inputObj, TransformerInfo transformerInfo) {
		Transformer<S, T> ar = getTransformer(transformerInfo);
		return (T) ar.transform(inputObj);
	}

	/**
	 * Gets the transformer.
	 *
	 * @param transformerInfo the transformer info
	 * @return the transformer
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws ClassNotFoundException the class not found exception
	 */
	private AbstractTransformer<S, T> getTransformer(TransformerInfo transformerInfo) {
		AbstractTransformer<S, T> instance = ReflectionUtils.createInstance(transformerInfo.getImplementation());
		instance.setTransformerInfo(transformerInfo);
		return instance;
	}

}