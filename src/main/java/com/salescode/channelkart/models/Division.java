/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salescode.channelkart.annotation.UniqueKey;
import com.salescode.channelkart.converters.RoleToStringConverter;
import com.salescode.channelkart.converters.StringToRoleConverter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * The class Division.
 *
 * @author Manish Srivastava
 * @since  July 2020
 * @version 1.1
 */
@Entity
@Table(name="ck_division",
uniqueConstraints = @UniqueConstraint(name="uk_division",columnNames = {"divisionName","parent"}))
public class Division extends CommonDataModel {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6260944281158172970L;

	/** The division name : Organization's user hierarchy  */
	@UniqueKey
	@NotBlank(message="division name must not be empty")
	@Column(length = 50)
	@Pattern(regexp="(^[ a-zA-Z0-9_-]*$)")
	private String divisionName;
	
	/** The display name. */
	private String displayName;
	
	/** The channel division. */
	private boolean channelDivision = false;
	
	/** The role access type. */
	private String roleAccessType;
	
	/** The permission group. */
	@JsonSerialize(converter = RoleToStringConverter.class)
	@JsonDeserialize(converter = StringToRoleConverter.class)
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ck_division_roles",
			joinColumns = @JoinColumn(name = "division_id"),
			inverseJoinColumns = @JoinColumn(name = "roles_id"))
	private List<Role> permissionGroups;
	
	/** The division parent : user hierarchy parent */
	@UniqueKey
	@Column(length = 50,columnDefinition = "varchar(50) default 'none'")
	private String parent="none";
	
	/** The level. */
	@JsonProperty(access = Access.READ_ONLY)
	private int level = 0;

	public String getRoleAccessType() {
		return roleAccessType;
	}

	public void setRoleAccessType(String roleAccessType) {
		this.roleAccessType = (roleAccessType!=null)?roleAccessType.toLowerCase():null;
	}

	/**
	 * Gets the division name.
	 *
	 * @return the division name
	 */
	public String getDivisionName() {
		return divisionName;
	}
	
	/**
	 * Gets the level.
	 *
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Sets the level.
	 *
	 * @param level the new level
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	
	/**
	 * Sets the division name.
	 *
	 * @param divisionName the new division name
	 */
	public void setDivisionName(String divisionName) {
		this.divisionName = (divisionName!=null)?divisionName.toLowerCase():null;
	}
	
	/**
	 * Gets the display name.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Sets the display name.
	 *
	 * @param displayName the new display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = (displayName!=null)?displayName.toLowerCase():null;
	}
	
	/**
	 * Checks if is channel division.
	 *
	 * @return true, if is channel division
	 */
	public boolean isChannelDivision() {
		return channelDivision;
	}
	
	/**
	 * Gets the channel division.
	 *
	 * @return the channel division
	 */
	public boolean getChannelDivision() {
		return this.channelDivision;
	}
	
	/**
	 * Sets the channel division.
	 *
	 * @param channelDivision the new channel division
	 */
	public void setChannelDivision(boolean channelDivision) {
		this.channelDivision = channelDivision;
	}
	
	/**
	 * @return the permissionGroup
	 */
	public List<Role> getPermissionGroups() {
		return permissionGroups;
	}

//	/**
//	 * @param permissionGroup the permissionGroup to set
//	 */
	public void setPermissionGroups(List<Role> permissionGroups) {
		this.permissionGroups = permissionGroups;
	}

	/**
	 * @return the parent
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(String parent) {
		this.parent = (StringUtils.isNotBlank(parent))?parent.toLowerCase():this.parent;
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((divisionName == null) ? 0 : divisionName.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
		if(obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		Division other = (Division) obj;
		if (divisionName == null) {
			if (other.divisionName != null)
				return false;
		} else if (!divisionName.equals(other.divisionName))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Division [divisionName=" + divisionName + ", parent=" + parent + "]";
	}	
	
}