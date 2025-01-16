package com.applicate.services.channelkart.validations;

public abstract class AbstractRule<T> implements Rule{
	
	private RuleInfo ruleInfo;
	public RuleInfo getRuleInfo() {
		return ruleInfo;
	}
	public void setRuleInfo(RuleInfo ruleInfo) {
		this.ruleInfo = ruleInfo;
	}
	protected AbstractRule(RuleInfo rule) {
		this.ruleInfo=rule;
	}
	protected AbstractRule() {
		this(null);
	}
	public abstract RuleResult apply(T cdm);

	/**
	 *
	 * @param rule used in overriden functions
	 * @param cdm
	 * @return
	 */
	public RuleResult apply(RuleInfo rule,T cdm){
		return apply(cdm);
	}

}
