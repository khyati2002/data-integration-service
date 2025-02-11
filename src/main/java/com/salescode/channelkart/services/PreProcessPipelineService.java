package com.salescode.channelkart.services;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.OperationResponse;
import com.salescode.dataintegration.etl.OperationResponse.OperationStatus;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.service.DataEntityValidationService;
import com.salescode.dataintegration.etl.validation.service.DataValidationService;

@Service
public class PreProcessPipelineService {

    private final DataEnrichmentService dataEnrichmentService;
    private final DataValidationService dataValidationService;
    private final DataEntityValidationService dataEntityValidationService;

    public PreProcessPipelineService(DataEnrichmentService dataEnrichmentService, DataValidationService dataValidationService, DataEntityValidationService dataEntityValidationService) {
        this.dataEnrichmentService = dataEnrichmentService;
        this.dataValidationService = dataValidationService;
        this.dataEntityValidationService = dataEntityValidationService;
    }

    @SuppressWarnings("unchecked")
    public <T extends CommonDataModel> OperationResponse process(T cdmIn) {
        return process(cdmIn, Optional.empty());
    }

    @SuppressWarnings("rawtypes")
    public OperationResponse process(CommonDataModel cdmIn, Optional<String> validationExcludeName) {
        return process(cdmIn, validationExcludeName, false);
    }

    @SuppressWarnings("rawtypes")
    public OperationResponse process(CommonDataModel cdmIn, Optional<String> validationExcludeName, boolean applyBatchValidation) {
        EnrichmentOperationResult er = TimerUtils.withTime("Time taken to pre validatoin enrichment: ", k -> dataEnrichmentService.enrich(cdmIn, EnrichmentPhase.PRE_VALIDATION));
        OperationResponse or = new OperationResponse();
        or.setEnrichment(er);
        List<CommonDataModel> cdms = er.getEnrichedData();
        boolean cStatus = er.getStatus().equals(EnrichmentResult.Status.OK);
        for (CommonDataModel cdm : cdms) {
            if (cStatus) {
                ValidationResult ev = TimerUtils.withTime("Time taken to perform entityValidation: ", k -> dataEntityValidationService.validate(List.of(cdm)));
                or.setEntityValidation(ev);
                cStatus = ev.getStatus().equals(ValidationResult.Status.OK);
            }
//            if (cStatus && applyBatchValidation) {
//                ValidationResult ebv = TimerUtils.withTime("Time taken to perform entityValidation: ", k -> data.batchValidate(cdmIn, validationExcludeName));
//                or.setValidation(ebv);
//                cStatus = ebv.getStatus().equals(ValidationResult.Status.OK);
//            }
            if (cStatus) {
                ValidationResult vr = TimerUtils.withTime("Time taken to perform validation: ", k -> dataValidationService.validate(List.of(cdm)));
                or.setValidation(vr);
                cStatus = vr.getStatus().equals(ValidationResult.Status.OK);
            }
            if (cStatus) {
                EnrichmentOperationResult erPost = TimerUtils.withTime("Time taken to perform post validation enrichment: ", k -> dataEnrichmentService.enrich(cdm, EnrichmentPhase.POST_VALIDATION));
                cStatus = erPost.getStatus().equals(EnrichmentResult.Status.OK);
                or.getEnrichment().setStatus(erPost.getStatus());
                or.getEnrichment().getEnrichmentResults().addAll(erPost.getEnrichmentResults());
            }
        }
        if (cStatus) {
            or.setStatus(OperationStatus.Success);
        } else {
            or.setStatus(OperationStatus.Failure);
        }
        return or;
    }
}
