package com.salescode.dataintegration.etl.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import static com.salescode.dataintegration.etl.validation.ValidationResult.Status;

@Getter
@Setter
public class RuleResult {

    public static final RuleResult OK = new RuleResult(Status.OK);
    public static final RuleResult ERROR = new RuleResult(Status.ERROR);
    public static final RuleResult WARNING = new RuleResult(Status.WARNING);
    private Status status;
    private String message;
    private String errorCode;
//    private RuleInfo rule;

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
