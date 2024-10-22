package com.salescode.channelkart.validations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
public class RuleResult {

    public static final RuleResult OK = new RuleResult(Status.OK);
    public static final RuleResult ERROR = new RuleResult(Status.ERROR);
    public static final RuleResult WARNING = new RuleResult(Status.WARNING);
    private final Status status;

    @Setter
    private String message;
    @Setter
    private String errorCode;
    @Setter
    private RuleInfo rule;

    @Setter
    @JsonIgnore
    private String exception;

    private RuleResult(Status status) {
        this(status, null);
    }

    public RuleResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public RuleResult(Status status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

}
