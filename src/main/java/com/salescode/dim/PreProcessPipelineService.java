package com.salescode.dim;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.validation.service.DataValidationService;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class PreProcessPipelineService implements Serializable {

    private static PreProcessPipelineService instance;
    private static final long serialVersionUID = 8463462672950445845L;
    private final static String SEPARATOR = ",";
    private final static String ENRICHMENT_ERROR = "Enrichment Failed : ";
    private final static String ENTITY_VALIDATION_ERROR = "EntityValidation Failed : ";
    private final static String VALIDATION_ERROR = "Validation Failed : ";
    private final transient DataEnrichmentService dataEnrichmentService;
    private final transient DataValidationService dataValidationService;


    public PreProcessPipelineService(DataValidationService dataValidationService, DataEnrichmentService dataEnrichmentService) {
        this.dataValidationService = dataValidationService;
        this.dataEnrichmentService = dataEnrichmentService;
    }

    public static synchronized PreProcessPipelineService getInstance(DataValidationService dataValidationService, DataEnrichmentService dataEnrichmentService){
        if(instance == null){
            instance = new PreProcessPipelineService(dataValidationService,dataEnrichmentService);
        }
        return instance;
    }

    public PreProcessOperationResult preProcessPipeline(CommonDataModel commonDataModel, String preprocessValidationExcludeGroup) {
        PreProcessOperationResult finalResult = new PreProcessOperationResult();

        OperationResult preValidationEnrich = dataEnrichmentService.enrich(commonDataModel, EnrichmentPhase.PRE_VALIDATION);
        finalResult.setPreValidationEnrichment(preValidationEnrich);

        if (finalResult.getPreValidationEnrichment().getStatus().equals(OperationResult.Status.OK)) {
            OperationResult validate = dataValidationService.validate(finalResult.getPreValidationEnrichment().getOperationResultData(), preprocessValidationExcludeGroup);
            finalResult.setValidation(validate);
        }

        if (finalResult.getValidation().getStatus().equals(OperationResult.Status.OK)) {
            OperationResult postValidationEnrich = dataEnrichmentService.enrich(finalResult.getValidation().getOperationResultData(), EnrichmentPhase.POST_VALIDATION);
            finalResult.setPostValidationEnrichment(postValidationEnrich);
        }

        if (finalResult.getPreValidationEnrichment().getStatus().equals(OperationResult.Status.OK) && finalResult.getValidation().getStatus().equals(OperationResult.Status.OK) && finalResult.getPostValidationEnrichment().getStatus().equals(OperationResult.Status.OK)) {
            finalResult.setStatus(PreProcessOperationResult.Status.SUCCESS);
        } else {
            finalResult.setStatus(PreProcessOperationResult.Status.FAILURE);
        }

        return finalResult;
    }

    public void evaluateFailures(PreProcessOperationResult response, List<String> errorList) {
        if (response.getPreValidationEnrichment() != null && response.getPreValidationEnrichment().getStatus().equals(OperationResult.Status.ERROR)) {
            List<OperationResult.StepResult> enrichmentResult = response.getPreValidationEnrichment().getStepResults();
            String error = StringUtils.join(enrichmentResult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(ENRICHMENT_ERROR + error);
        }
        if (response.getValidation() != null && response.getValidation().getStatus().equals(OperationResult.Status.ERROR)) {
            List<OperationResult.StepResult> ruleresult = response.getValidation().getStepResults();
            String error = StringUtils.join(ruleresult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(VALIDATION_ERROR + error);
        }
        if (response.getPostValidationEnrichment() != null && response.getPostValidationEnrichment().getStatus().equals(OperationResult.Status.ERROR)) {
            List<OperationResult.StepResult> enrichmentResult = response.getPostValidationEnrichment().getStepResults();
            String error = StringUtils.join(enrichmentResult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(ENRICHMENT_ERROR + error);
        }
    }
}