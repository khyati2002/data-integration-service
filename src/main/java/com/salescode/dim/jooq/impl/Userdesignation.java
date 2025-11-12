package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Userdesignation implements Serializable {

	private String loginId;
	private String designation;

	public Userdesignation() {
		// Default constructor
	}

	public Userdesignation(String loginId, String designation) {
		this.loginId = loginId;
		this.designation = designation;
	}

	@Override
	public String toString() {
		return "Userdesignation{" +
				"loginId='" + loginId + '\'' +
				", designation='" + designation + '\'' +
				'}';
	}
}
