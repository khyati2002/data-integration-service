package com.applicate.services.channelkart.exceptions;

import lombok.Getter;

/**
 * Custom exception for Loadout batch save operations.
 * This exception is thrown when errors occur during the batch save process
 * for Loadout entities and their hierarchical children (LoadoutDetails and LoadoutItems).
 * 
 * Extends RuntimeException to allow it to be thrown from methods that override
 * interfaces not declaring checked exceptions.
 */
@Getter
public class LoadoutBatchSaveException extends RuntimeException {

    private final ErrorType errorType;
    private final String context;

    /**
     * Error types for categorizing batch save failures
     */
    public enum ErrorType {
        VALIDATION_ERROR,
        DATABASE_ERROR,
        SEQUENCE_GENERATION_ERROR,
        HIERARCHY_PREPARATION_ERROR,
        TRANSACTION_ERROR
    }

    /**
     * Constructs a new LoadoutBatchSaveException with the specified detail message.
     *
     * @param message the detail message
     * @param errorType the type of error that occurred
     */
    public LoadoutBatchSaveException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.context = null;
    }

    /**
     * Constructs a new LoadoutBatchSaveException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param errorType the type of error that occurred
     * @param cause the cause of the exception
     */
    public LoadoutBatchSaveException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.context = null;
    }

    /**
     * Constructs a new LoadoutBatchSaveException with the specified detail message, error type, and context.
     *
     * @param message the detail message
     * @param errorType the type of error that occurred
     * @param context additional context information about the error
     */
    public LoadoutBatchSaveException(String message, ErrorType errorType, String context) {
        super(message);
        this.errorType = errorType;
        this.context = context;
    }

    /**
     * Constructs a new LoadoutBatchSaveException with the specified detail message, error type, context, and cause.
     *
     * @param message the detail message
     * @param errorType the type of error that occurred
     * @param context additional context information about the error
     * @param cause the cause of the exception
     */
    public LoadoutBatchSaveException(String message, ErrorType errorType, String context, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.context = context;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append(": [").append(errorType).append("]");
        if (context != null) {
            sb.append(" Context: ").append(context);
        }
        String message = getLocalizedMessage();
        if (message != null) {
            sb.append(" - ").append(message);
        }
        return sb.toString();
    }
}
