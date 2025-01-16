package com.applicate.services.channelkart.validations;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EntityValidationResult {

    public static final EntityValidationResult OK = new EntityValidationResult(Status.OK);
    public static final EntityValidationResult ERROR = new EntityValidationResult(Status.ERROR);
    private Status status;
    private String message;

    public EntityValidationResult(Status status) {
        this(status, null);
    }

    public EntityValidationResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isError() {
        return status != Status.OK;
    }


}
