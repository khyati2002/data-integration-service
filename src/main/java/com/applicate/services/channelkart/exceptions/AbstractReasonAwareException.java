package com.applicate.services.channelkart.exceptions;

public abstract class AbstractReasonAwareException extends RuntimeException implements ReasonAwareException{

    private final ErrorCode errorCode;

    protected AbstractReasonAwareException(Throwable cause) {
        super(cause);
        this.errorCode = null;
    }

    protected AbstractReasonAwareException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    protected AbstractReasonAwareException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AbstractReasonAwareException(String message) {
        super(message);
        this.errorCode = null;
    }

    protected AbstractReasonAwareException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    protected AbstractReasonAwareException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = null;
    }

    protected AbstractReasonAwareException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    protected AbstractReasonAwareException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    protected AbstractReasonAwareException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    protected AbstractReasonAwareException() {
        this.errorCode = null;
    }


    @Override
    public String getReason(){

        if(getMessage() != null){
            return getMessage();
        }
        if(errorCode != null){
            return errorCode.getReason();
        }
        return "Something went wrong";
    }

    @Override
    public String getErrorCode() {
        if(errorCode != null){
            return errorCode.getCode();
        }
        return null;
    }

}
