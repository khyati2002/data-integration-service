package com.applicate.services.channelkart.response;

import com.applicate.services.channelkart.enrichments.EnrichmentOperationResult;
import com.applicate.services.channelkart.utils.CollectionUtils;
import com.applicate.services.channelkart.validations.EntityValidationResult;
import com.applicate.services.channelkart.validations.ValidationResult;
import java.util.List;

public class OperationResponse<T>  {

    private ValidationResult validation;

    private EnrichmentOperationResult enrichment;

    private EntityValidationResult entityValidation;

    public EnrichmentOperationResult getEnrichment() {
        return enrichment;
    }

    public EntityValidationResult getEntityValidation() {
        return entityValidation;
    }

    public void setEntityValidation(EntityValidationResult entityValidation) {
        this.entityValidation = entityValidation;
    }

    public void setEnrichment(EnrichmentOperationResult enrichment) {
        this.enrichment = enrichment;
    }

    private OperationStatus status;

    private T feature;

    public T getFeature() {
        return feature;
    }

    public void setFeature(T feature) {
        this.feature = feature;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(OperationStatus status) {
        this.status = status;
    }

    public ValidationResult getValidation() {
        return validation;
    }
    private List<T> processedItems;
    private List<String> errors;
    private String errorCode;

    public List<T> getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(List<T> processedItems) {
        this.processedItems = processedItems;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean hasValidationErrors() {
        return (this.validation != null && CollectionUtils.isNotEmpty(this.validation.getViolations()))
                || (this.entityValidation != null && this.entityValidation.isError());
    }

    public void setValidation(ValidationResult validation) {
        this.validation = validation;
    }

}
