/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.models;



import com.salescode.channelkart.annotation.UniqueKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * The Class UserParent.
 * 
 * @author Manish Srivastava
 * @since  Jun 2020
 */
@Entity
@Table(name="ck_user_parent"
       ,uniqueConstraints = {@UniqueConstraint(name = "uk_user_parent",columnNames = { "userloginid", "parent" })})
public class UserParent extends CommonDataModel{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1711216736915908881L;
	
	/** The immediate parent. */
	@UniqueKey
	@Column(name = "userloginid")
	private String userLoginId;
	
	/** The parent. */
	@UniqueKey
	@Column(name = "parent")
	private String parent;

	/**
	 * Gets the user login id.
	 *
	 * @return the user login id
	 */
	public String getUserLoginId() {
		return userLoginId;
	}

	/**
	 * Sets the user login id.
	 *
	 * @param userLoginId the new user login id
	 */
	public void setUserLoginId(String userLoginId) {
		this.userLoginId = userLoginId;
	}

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Sets the parent.
	 *
	 * @param parent the new parent
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		int result = ((parent == null) ? 0 : parent.hashCode());
		result = result + ((userLoginId == null) ? 0 : userLoginId.hashCode());
		return result;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		UserParent other = (UserParent) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (userLoginId == null) {
			if (other.userLoginId != null)
				return false;
		} else if (!userLoginId.equals(other.userLoginId))
			return false;
		return true;
	}

}
