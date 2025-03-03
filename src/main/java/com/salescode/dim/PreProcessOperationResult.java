package com.salescode.dim;


import com.salescode.dim.etl.OperationResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreProcessOperationResult {
    private OperationResult preValidationEnrichment;
    private OperationResult validation;
    private OperationResult postValidationEnrichment;

    private Status status;

    public enum Status {
        SUCCESS, FAILURE
    }

}