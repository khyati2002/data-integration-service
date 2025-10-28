package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
public class AccountInfo extends com.salescode.dim.jooq.generated.tables.pojos.AccountInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/***
	 * Login id of the related user
	 */
	private String accountId;

	/***
	 * Bank account number
	 */
	private String accountNumber;
	
	private String bank;

	private String shipToGst;

	private String shipToPan;





	private String mobile;


	private String loginId;


	private String accountPayload;

	/***
	 * IFSC code
	 */
	@Transient
	private String ifsc;

	@Transient
	private String bankName;

	@Transient
	private String addressLine1;

	@Transient
	private String addressLine2;

	
	private String email;

	@Transient
	private String constitution;

	
	private String contactName;

	/**
	 * Any extra attributes which are not part of the standard schema is stored here in json format
	 */
	@Transient
	public transient JsonNode extraAttributes;


	private String authPersonName;

	@Transient
	private String pan;

	@Transient
	private String gstin;

	@Transient
	private String checkLeafURL;
	
	@Transient
	private String accountHolderName;


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		AccountInfo that = (AccountInfo) o;

		if (!accountId.equals(that.accountId)) return false;
		if (!accountNumber.equals(that.accountNumber)) return false;
		if (!loginId.equals(that.loginId)) return false;
		return accountPayload.equals(that.accountPayload);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + accountId.hashCode();
		result = 31 * result + accountNumber.hashCode();
		result = 31 * result + loginId.hashCode();
		result = 31 * result + accountPayload.hashCode();
		return result;
	}



}
