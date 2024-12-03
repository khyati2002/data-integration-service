package com.salescode.channelkart.response;

import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OperationResponse {

    private ValidationResult validation;

    private EnrichmentOperationResult enrichment;

    private ValidationResult entityValidation;

    private OperationStatus status;







    public enum OperationStatus {

        Success, Failure, PREPROCESS_PIPELINE_FAILURE, INTERNAL_SERVER_FAILURE, DATABASE_FAILURE;

        public static OperationStatus from(boolean isSuccess) {
            return isSuccess ? Success : Failure;
        }

        public boolean isSuccess() {
            return this == Success;
        }

    }

}
