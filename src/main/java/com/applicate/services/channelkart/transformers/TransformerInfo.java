/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.transformers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.applicate.services.channelkart.converters.JSONArrayConverter;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.Language;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Objects;

/**
 * The class TransformerInfo.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
@Entity
@Table(name="ck_transformer_info",
uniqueConstraints = @UniqueConstraint(name="uk_transformer_info",columnNames = {"name"}))
public class TransformerInfo extends CommonDataModel implements Comparator<TransformerInfo>, Comparable<TransformerInfo>{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The code. */
	@Column(columnDefinition = "json")
	@Convert(converter = JSONArrayConverter.class)
	private ArrayNode code;
	
	/** The description. */
	private String description;
	
	/** The document link. */
	private String documentLink;
	
	/** The severity. */
	private int severity;
	
	/** The priority. */
	private int priority;
	
	/** The enabled. */
	private boolean enabled;
	
	/** The type. */
	private String type;
	
	/** The language. */
	@Enumerated(EnumType.STRING)
	@NotNull
	private Language language;
	
	/** The implementation. */
	private String implementation;
	
	/** The transformer name. */
	@NotNull
	@NotBlank
	private String name;

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public ArrayNode getCode() {
		return code;
	}
	
	/**
	 * Sets the code.
	 *
	 * @param code the new code
	 */
	public void setCode(ArrayNode code) {
		this.code = code;
	}
	
	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets the description.
	 *
	 * @param description the new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets the document link.
	 *
	 * @return the document link
	 */
	public String getDocumentLink() {
		return documentLink;
	}
	
	/**
	 * Sets the document link.
	 *
	 * @param documentLink the new document link
	 */
	public void setDocumentLink(String documentLink) {
		this.documentLink = documentLink;
	}
	
	/**
	 * Gets the severity.
	 *
	 * @return the severity
	 */
	public int getSeverity() {
		return severity;
	}
	
	/**
	 * Sets the severity.
	 *
	 * @param severity the new severity
	 */
	public void setSeverity(int severity) {
		this.severity = severity;
	}
	
	/**
	 * Gets the priority.
	 *
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}
	
	/**
	 * Sets the priority.
	 *
	 * @param priority the new priority
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	/**
	 * Gets the enabled.
	 *
	 * @return the enabled
	 */
	public boolean getEnabled() {
		return enabled;
	}
	
	/**
	 * Checks if is enabled.
	 *
	 * @return true, if is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Sets the enabled.
	 *
	 * @param enabled the new enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * Gets the language.
	 *
	 * @return the language
	 */
	public Language getLanguage() {
		return language;
	}
	
	/**
	 * Sets the language.
	 *
	 * @param language the new language
	 */
	public void setLanguage(Language language) {
		this.language = language;
	}
	
	/**
	 * Gets the implementation.
	 *
	 * @return the implementation
	 */
	public String getImplementation() {
		return implementation;
	}
	
	/**
	 * Sets the implementation.
	 *
	 * @param implementation the new implementation
	 */
	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Sets the type.
	 *
	 * @param type the new type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Compare.
	 *
	 * @param r1 the r 1
	 * @param r2 the r 2
	 * @return the int
	 */
	@Override
	public int compare(TransformerInfo r1, TransformerInfo r2) {
		if(r1.getPriority()<r2.getPriority())
			return 1;
		else if (r1.getPriority()>r2.getPriority())
			return -1;
		return 0;
	}

	@Override
	public String toString() {
		return "TransformerInfo{" +
				"description='" + description + '\'' +
				", enabled=" + enabled +
				", type='" + type + '\'' +
				", language=" + language +
				", implementation='" + implementation + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		TransformerInfo that = (TransformerInfo) o;

		if (severity != that.severity) return false;
		if (priority != that.priority) return false;
		if (enabled != that.enabled) return false;
		if (!Objects.equals(code, that.code)) return false;
		if (!Objects.equals(type, that.type)) return false;
		if (language != that.language) return false;
		return Objects.equals(implementation, that.implementation);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (code != null ? code.hashCode() : 0);
		result = 31 * result + severity;
		result = 31 * result + priority;
		result = 31 * result + (enabled ? 1 : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (language != null ? language.hashCode() : 0);
		result = 31 * result + (implementation != null ? implementation.hashCode() : 0);
		return result;
	}

	@Override
	public int compareTo(TransformerInfo that) {
		return Integer.compare(that.priority, this.priority);
	}


}

