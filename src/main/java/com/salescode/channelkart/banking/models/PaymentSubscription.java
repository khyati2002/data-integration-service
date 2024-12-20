package com.salescode.channelkart.banking.models;



import com.salescode.channelkart.models.CommonDataModel;

import javax.persistence.*;

@Entity
@Table(name = "ck_payment_subscription",
		indexes={
				@Index(name="ck_payment_subscription_idx_1",columnList="userId"),
				@Index(name="ck_payment_subscription_idx_2",columnList="paymentProvideType")
		})
public class PaymentSubscription extends CommonDataModel {

	private static final long serialVersionUID = 1L;

	/**
	 * Bank type finagg/axis/sbi
	 */
	private String paymentProvideType;

	private String userId;

	private String accountId;

	private String supplier;

	private String isEnabled;

	private String paymentEnabled;
	
//	@Enumerated(EnumType.STRING)
//	private PaymentType paymentType;

	public String getPaymentProvideType() {
		return paymentProvideType;
	}

	public void setPaymentProvideType(String paymentProvideType) {
		this.paymentProvideType = paymentProvideType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	private Status status;

	public String getSupplier() {
		return supplier;
	}

	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	public String getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(String isEnabled) {
		this.isEnabled = isEnabled;
	}

	public String getPaymentEnabled() {
		return paymentEnabled;
	}

	public void setPaymentEnabled(String paymentEnabled) {
		this.paymentEnabled = paymentEnabled;
	}

//	public PaymentType getPaymentType() {
//		return paymentType;
//	}
//
//	public void setPaymentType(PaymentType paymentType) {
//		this.paymentType = paymentType;
//	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		PaymentSubscription that = (PaymentSubscription) o;

		if (paymentProvideType != null ? !paymentProvideType.equals(that.paymentProvideType)
				: that.paymentProvideType != null) {
			return false;
		}
		if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
			return false;
		}
		if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) {
			return false;
		}
	return status == that.status;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (paymentProvideType != null ? paymentProvideType.hashCode() : 0);
		result = 31 * result + (userId != null ? userId.hashCode() : 0);
		result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
	    result = 31 * result + (status != null ? status.hashCode() : 0);
		return result;
	}
}
