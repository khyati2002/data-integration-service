package com.salescode.dataintegration.etl.validation;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ValidationResult {

    private Status status;
    private List<RuleResult> successMessages = new ArrayList<>();
    private List<RuleResult> violations = new ArrayList<>();

    public ValidationResult(Status status) {
        this.status = status;
    }

    public enum Status {

        OK, ERROR, WARNING, CONFLICT

    }


}
