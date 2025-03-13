package com.salescode.dim;


import com.salescode.dim.etl.OperationResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreProcessOperationResult {
    private OperationResult preValidationEnrichment = OperationResult.OK;
    private OperationResult validation = OperationResult.OK;
    private OperationResult postValidationEnrichment = OperationResult.OK;

    private Status status;

    public enum Status {
        SUCCESS, FAILURE
    }

}