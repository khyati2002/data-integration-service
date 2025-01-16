/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.pojo;

import java.lang.reflect.Field;

/**
 * The class UniqueKeyContainer.
 *
 * @author Manish Srivastava
 * @since  Jun 2020
 */
public class UniqueKeyContainer {

	/** The field. */
	private Field field;
	
	/** The native name. */
	private String nativeName;
	
	/** The path. */
	private String path;

	/**
	 * Gets the field.
	 *
	 * @return the field
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Sets the field.
	 *
	 * @param field the new field
	 */
	public void setField(Field field) {
		this.field = field;
	}

	/**
	 * Gets the native name.
	 *
	 * @return the native name
	 */
	public String getNativeName() {
		return nativeName;
	}

	/**
	 * Sets the native name.
	 *
	 * @param nativeName the new native name
	 */
	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the path.
	 *
	 * @param path the new path
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
}
