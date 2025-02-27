package com.salescode.dim.etl;

public interface ProcessResult {

    OperationResult.Status getStatus();

    void setStatus(OperationResult.Status status);

    String getMessage();

    void setMessage(String message);

}
