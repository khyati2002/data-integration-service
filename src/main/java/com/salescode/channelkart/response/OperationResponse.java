package com.salescode.channelkart.response;

import com.salescode.channelkart.enrichments.EnrichmentOperationResult;
import com.salescode.channelkart.utils.CollectionUtils;
import com.salescode.channelkart.validations.EntityValidationResult;
import com.salescode.channelkart.validations.ValidationResult;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OperationResponse<T> {

    private ValidationResult validation;

    private EnrichmentOperationResult enrichment;

    private EntityValidationResult entityValidation;
    private OperationStatus status;
    private T feature;

    public boolean hasValidationErrors() {
        return (this.validation != null && CollectionUtils.isNotEmpty(this.validation.getViolations())) || (this.entityValidation != null && this.entityValidation.isError());
    }

}
