package com.applicate.services.channelkart.models;


import com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.converters.JSONObjectConverter;
import com.applicate.services.channelkart.models.enums.SubscriptionPlan;

import javax.persistence.*;

@Entity
@Table(name = "ck_customer_account")
public class CustomerAccountInfo extends CommonDataModel{
	private static final long serialVersionUID = 1L;
	/**
	 * Name of the customer
	 */
	private String name;

	/**
	 * Default admin user for the account
	 * 
	 * @see User
	 */
	
	@OneToOne(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "username", referencedColumnName = "loginid", nullable = false)
	private User admin;

	//String username =admin.getLoginId();

	/**
	 * The plan to which the customer has subscribed for. Based on the plan, the
	 * features would be made available in the App and Portal
	 * 
	 * @see SubscriptionPlan
	 */
	@Enumerated(EnumType.STRING)
	private SubscriptionPlan subscriptionPlan;

	private String timeZone;

	private String currency;

	private String language;

	private String currencySymbol;

	/** Name of the Company */
	private String companyName;

	/** Address of the Company */
	private String comapnyAddress;

	/** Company Pan Number */
	private String companyPan;

	/** Corporate Identity Number */
	private String companyCin;

	/** Company Tax Numbers */
	@Column(columnDefinition = "json")
	@Convert(converter = JSONObjectConverter.class)
	private JsonNode taxNumber;


	public User getAdmin() {
		return admin;
	}

	public void setAdmin(User admin) {
		this.admin = admin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SubscriptionPlan getSubscriptionPlan() {
		return subscriptionPlan;
	}

	public CustomerAccountInfo setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
		this.subscriptionPlan = subscriptionPlan;
		return this;
	}

	/**
	 * @return the timeZone
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * @param timeZone the timeZone to set
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCurrencySymbol() {
		return currencySymbol;
	}

	public void setCurrencySymbol(String currencySymbol) {
		this.currencySymbol = currencySymbol;
	}

	/**
	 * @return the companyName
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @param companyName the companyName to set
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @return the comapnyAddress
	 */
	public String getComapnyAddress() {
		return comapnyAddress;
	}

	/**
	 * @param comapnyAddress the comapnyAddress to set
	 */
	public void setComapnyAddress(String comapnyAddress) {
		this.comapnyAddress = comapnyAddress;
	}

	/**
	 * @return the companyPan
	 */
	public String getCompanyPan() {
		return companyPan;
	}

	/**
	 * @param companyPan the companyPan to set
	 */
	public void setCompanyPan(String companyPan) {
		this.companyPan = companyPan;
	}

	/**
	 * @return the companyCin
	 */
	public String getCompanyCin() {
		return companyCin;
	}

	/**
	 * @param companyCin the companyCin to set
	 */
	public void setCompanyCin(String companyCin) {
		this.companyCin = companyCin;
	}

	/**
	 * @return the taxNumber
	 */
	public JsonNode getTaxNumber() {
		return taxNumber;
	}

	/**
	 * @param taxNumber the taxNumber to set
	 */
	public void setTaxNumber(JsonNode taxNumber) {
		this.taxNumber = taxNumber;
	}
}
