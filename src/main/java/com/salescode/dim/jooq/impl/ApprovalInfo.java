package com.salescode.dim.jooq.impl;

import java.io.Serializable;

public class ApprovalInfo {
	private String referenceId;
	private Status status;
	/**
	 * remark is remarks given by specific user as per role
	 */
	private String remark;

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public String toString() {
		return "ApprovalInfo{" + "referenceId='" + referenceId + '\'' + ", status=" + status + ", remark='" + remark + '\'' + '}';
	}

	public enum Status {
		PENDING, APPROVED, REJECTED, REVERSE, PROCESSED_BY_OTHERS, PARTIALLYAPPROVED
	}
}
