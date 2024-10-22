package com.salescode.channelkart.validations;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class AbstractRule<T> implements Rule {

    private RuleInfo ruleInfo;

    protected AbstractRule(RuleInfo rule) {
        this.ruleInfo = rule;
    }

    protected AbstractRule() {
        this(null);
    }

    public abstract RuleResult apply(T cdm);

    public RuleResult apply(RuleInfo rule, T cdm) {
        return apply(cdm);
    }

}
