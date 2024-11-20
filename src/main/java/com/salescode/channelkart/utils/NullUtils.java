/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.utils;

import org.json.JSONArray;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * The class NullUtils.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class NullUtils {

	private NullUtils() {}

	/**
	 * Checks for null values.
	 *
	 * @param args the args
	 * @return true, if successful
	 */
	public static boolean hasNullValues(Object... args) {
		for (Object item : args) {
			if (isNull(item)) {
				return true;
			}
		}
		return false;
	}

	public static void requireNonNull(Supplier<? extends RuntimeException> exceptionSupplier, Object... args) {
		if (hasNullValues(args)) {
			throw exceptionSupplier.get();
		}
	}

	public static void requireNonNull(Function<String, ? extends RuntimeException> function, Object... args) {
		if (hasNullValues(args)) {
			String argsInString = Arrays.toString(args);
			function.apply(argsInString);
		}
	}

	/**
	 * Checks for null values.
	 *
	 * @param array the array
	 * @return true, if successful
	 */
	public static boolean hasNullValues(JSONArray array) {
		for(int i = 0; i < array.length(); i++) {
			if(array.opt(i) == null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if is null.
	 *
	 * @param item the item
	 * @return true, if is null
	 */
	public static boolean isNull(Object item) {
		return  item == null;
	}

	/**
	 * Checks if is not null.
	 *
	 * @param item the item
	 * @return true, if is not null
	 */
	public static boolean isNotNull(Object item) {
		return !isNull(item);
	}

	public static <T> T getOrDefault(Supplier<T> supplier, T defaultValue) {
		T t = supplier.get();
		return t == null ? defaultValue : t;
	}

}
