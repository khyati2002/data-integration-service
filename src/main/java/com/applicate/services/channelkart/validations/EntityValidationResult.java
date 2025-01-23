package com.applicate.services.channelkart.validations;

public class EntityValidationResult {

    public  static final EntityValidationResult OK = new EntityValidationResult(Status.OK);
    public  static final EntityValidationResult ERROR = new EntityValidationResult(Status.ERROR);
    private Status status;
    private String message;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public EntityValidationResult(Status status) {
        this(status,null);
    }
    public EntityValidationResult(Status status,String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isError() {
        return status != Status.OK;
    }


}
