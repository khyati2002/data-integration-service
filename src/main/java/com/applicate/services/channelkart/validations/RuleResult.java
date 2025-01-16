package com.applicate.services.channelkart.validations;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RuleResult {
	
	public static final RuleResult OK = new RuleResult(Status.OK);
    public static final RuleResult ERROR = new RuleResult(Status.ERROR);
    public static final RuleResult WARNING = new RuleResult(Status.WARNING);
	private Status status;
	private String message;
	private String errorCode;
	private RuleInfo rule;

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	@JsonIgnore
	private String exception;

	public void setMessage(String message) {
		this.message = message;
	}

	public RuleInfo getRule() {
		return rule;
	}

	public void setRule(RuleInfo rule) {
		this.rule = rule;
	}

	public Status getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
    private RuleResult(Status status) {
       this(status,null);
    }
    
    public RuleResult(Status status,String message) {
        this.status = status;
        this.message = message;
    }
    
    public RuleResult(Status status,String message,String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
   
}
