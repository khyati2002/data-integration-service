/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.component.model;

/**
 * The interface AbstractExecutor.
 *
 * @author Manish Srivastava
 * @param <T> the generic type
 * @since  May 2020
 */
public interface AbstractExecutor<K,T> {

	/** The  threads count. */
	public int _THREADS_COUNT= 10;
	 
	/**
	 * Execute.
	 *
	 * @param arg the arg
	 * @param objects the objects
	 * @return the object
	 */
	public K execute(T arg, Object...objects);
	
}
