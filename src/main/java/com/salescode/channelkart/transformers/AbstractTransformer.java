/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.transformers;

/**
 * The class AbstractTransformer.
 *
 * @author Manish Srivastava
 * @param <S> the generic type
 * @param <T> the generic type
 * @since  May 2020
 */
public abstract class AbstractTransformer<S,T> implements Transformer<S,T>{
	
	/** The transformer info. */
	private TransformerInfo transformerInfo;
	
	/**
	 * Gets the transformer info.
	 *
	 * @return the transformer info
	 */
	public TransformerInfo getTransformerInfo() {
		return transformerInfo;
	}
	
	/**
	 * Sets the transformer info.
	 *
	 * @param transformerInfo the new transformer info
	 */
	public void setTransformerInfo(TransformerInfo transformerInfo) {
		this.transformerInfo = transformerInfo;
	}
	
	/**
	 * Instantiates a new abstract transformer.
	 *
	 * @param transformerInfo the transformer info
	 */
	protected AbstractTransformer(TransformerInfo transformerInfo) {
		this.transformerInfo=transformerInfo;
	}
	
	/**
	 * Instantiates a new abstract transformer.
	 */
	protected AbstractTransformer() {
		this(null);
	}

	/**
	 * Source type.
	 *
	 * @return the class
	 */
	@SuppressWarnings("unchecked")
	public Class<S> sourceType(){
		return (Class<S>) getClass();
	}
	
}
