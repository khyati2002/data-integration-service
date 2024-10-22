package com.salescode.channelkart.validations;

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


}
