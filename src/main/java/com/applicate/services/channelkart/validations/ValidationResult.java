package com.applicate.services.channelkart.validations;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult  {

	private Status status;
	private List<RuleResult> successMessages=new ArrayList<>();
	private List<RuleResult> violations=new ArrayList<>();
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public ValidationResult(Status status) {
		this.status=status;
	}
	public List<RuleResult> getSuccessMessages() {
		return successMessages;
	}

	public void setSuccessMessages(List<RuleResult> successMessages) {
		this.successMessages = successMessages;
	}
	public List<RuleResult> getViolations() {
		return violations;
	}
	public void setViolations(List<RuleResult> violations) {
		this.violations = violations;
	}


}
