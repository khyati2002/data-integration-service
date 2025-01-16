package com.applicate.services.channelkart.response;

import com.applicate.services.channelkart.enrichments.EnrichmentOperationResult;
import com.applicate.services.channelkart.utils.CollectionUtils;
import com.applicate.services.channelkart.validations.EntityValidationResult;
import com.applicate.services.channelkart.validations.ValidationResult;
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
