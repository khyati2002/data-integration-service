package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.enrichments.*;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.validations.*;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.response.OperationResponse;
import com.applicate.services.channelkart.response.OperationStatus;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreProcessPipelineService {
    private static final String ENRICHMENT_ERROR = "Enrichment : {}";
    private static final String ENTITY_VALIDATION_ERROR = "EntityValidation : {}";
    private static final String VALIDATION_ERROR = "Validation : {}";
    private static final String SEPERATOR = ",";
    private static final String DEFAULT_ERROR = "";
    @Autowired
    private DataEnrichmentService enrichmentService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private EntityValidationService entityValidationService;

    @SuppressWarnings("unchecked")
    public <T extends CommonDataModel> OperationResponse<T> process(T cdmIn) {
        return process(cdmIn, Optional.empty());
    }

    @SuppressWarnings("rawtypes")
    public OperationResponse process(CommonDataModel cdmIn, Optional<String> validationExcludeName) {
        return process(cdmIn, validationExcludeName, false);
    }

    @SuppressWarnings("rawtypes")
    public OperationResponse process(CommonDataModel cdmIn, Optional<String> validationExcludeName, boolean applyBatchValidation) {
        EnrichmentOperationResult er = TimerUtils.withTime("Time taken to pre validatoin enrichment: ", k -> enrichmentService.enrich(cdmIn, EnrichmentPhase.PRE_VALIDATION));
        OperationResponse or = new OperationResponse();
        or.setEnrichment(er);
        List<CommonDataModel> cdms = er.getEnrichedData();
        boolean cStatus = er.getStatus().equals(Status.OK);
        for (CommonDataModel cdm : cdms) {
            if (cStatus) {
                EntityValidationResult ev = TimerUtils.withTime("Time taken to perform entityValidation: ", k -> entityValidationService.validate(cdm));
                or.setEntityValidation(ev);
                cStatus = ev.getStatus().equals(com.applicate.services.channelkart.validations.Status.OK);
            }
            if (cStatus && applyBatchValidation) {
                ValidationResult ebv = TimerUtils.withTime("Time taken to perform entityValidation: ", k -> validationService.batchValidate(cdmIn, validationExcludeName));
                or.setValidation(ebv);
                cStatus = ebv.getStatus().equals(com.applicate.services.channelkart.validations.Status.OK);
            }
            if (cStatus) {
                ValidationResult vr = TimerUtils.withTime("Time taken to perform validation: ", k -> validationService.validate(cdm, validationExcludeName));
                or.setValidation(vr);
                cStatus = vr.getStatus().equals(com.applicate.services.channelkart.validations.Status.OK);
            }
            if (cStatus) {
                EnrichmentOperationResult erPost = TimerUtils.withTime("Time taken to perform post validation enrichment: ", k -> enrichmentService.enrich(cdm, EnrichmentPhase.POST_VALIDATION));
                cStatus = erPost.getStatus().equals(Status.OK);
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

    public String getError(@SuppressWarnings("rawtypes") OperationResponse response) {
        List<String> errorList = null;

        if (!response.getStatus().equals(OperationStatus.Failure)) {
            return DEFAULT_ERROR;
        }

        errorList = new ArrayList<>();
        if (response.getEnrichment() != null && response.getEnrichment().getStatus().equals(Status.ERROR)) {
            List<EnrichmentResult> enrichmentResult = response.getEnrichment().getEnrichmentResults();
            String error = org.apache.commons.lang.StringUtils.join(enrichmentResult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPERATOR);
            errorList.add(StringUtils.format(ENRICHMENT_ERROR, error));
        }
        if (response.getEntityValidation() != null && response.getEntityValidation().getStatus().equals(com.applicate.services.channelkart.validations.Status.ERROR)) {
            EntityValidationResult enrichmentResult = response.getEntityValidation();
            errorList.add(StringUtils.format(ENTITY_VALIDATION_ERROR, enrichmentResult.getMessage()));
        }
        if (response.getValidation() != null && response.getValidation().getStatus().equals(com.applicate.services.channelkart.validations.Status.ERROR)) {
            List<RuleResult> ruleresult = response.getValidation().getViolations();
            String error = org.apache.commons.lang.StringUtils.join(ruleresult.stream().map(mapper -> StringUtils.isNotBlank(mapper.getMessage()) ? mapper.getMessage() : mapper.getException()).collect(Collectors.toList()), SEPERATOR);
            errorList.add(StringUtils.format(VALIDATION_ERROR, error));
        }

        if (!errorList.isEmpty()) {
            return org.apache.commons.lang.StringUtils.join(errorList, SEPERATOR);
        }

        return DEFAULT_ERROR;
    }

}
