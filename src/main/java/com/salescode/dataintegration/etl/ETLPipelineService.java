package com.salescode.dataintegration.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.IntegrationHistory;
import com.salescode.channelkart.models.enums.EnrichmentPhase;
import com.salescode.channelkart.repository.IntegrationHistoryRepository;
import com.salescode.channelkart.response.OperationResponse;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.dto.StreamingRawData;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dataintegration.etl.transformer.service.DataTransformationService;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.service.DataEntityValidationService;
import com.salescode.dataintegration.etl.validation.service.DataValidationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLPipelineService {

    private static final String ENRICHMENT_ERROR = "Enrichment Failed : {}";
    private static final String ENTITY_VALIDATION_ERROR = "EntityValidation Failed : {}";
    private static final String VALIDATION_ERROR = "Validation Failed : {}";
    private static final String SAVE_ERROR = "Error while saving record. Reason: {}";
    private static final String SEPARATOR = ",";
    private final DataTransformationService dataTransformationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final DataValidationService dataValidationService;
    private final DataEntityValidationService dataEntityValidationService;
    private final IntegrationHistoryRepository integrationHistoryRepository;
    ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    public ETLPipelineService(DataTransformationService dataTransformationService, DataEnrichmentService dataEnrichmentService, DataValidationService dataValidationService, DataEntityValidationService dataEntityValidationService, IntegrationHistoryRepository integrationHistoryRepository, IntegrationHistoryRepository integrationHistoryRepository1) {
        this.dataTransformationService = dataTransformationService;
        this.dataEnrichmentService = dataEnrichmentService;
        this.dataValidationService = dataValidationService;
        this.dataEntityValidationService = dataEntityValidationService;
        this.integrationHistoryRepository = integrationHistoryRepository1;
    }

    @SneakyThrows
    public List<CommonDataModel> execute(String message) {
        log.info("Executing etl pipeline");
        List<CommonDataModel> transformedObjects = new ArrayList<>();
        StreamingRawData streamingRawData = objectMapper.readValue(message, StreamingRawData.class);
        ArrayNode features = streamingRawData.getFeatures();
        if (features.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be empty");
        }
        for (JsonNode jsonNode : features) {
            try{
                streamingRawData.setFeatures(objectMapper.createArrayNode().add(jsonNode));
                transformedObjects.addAll(process(streamingRawData));
            } catch (Exception e) {
                integrationHistoryRepository.save(IntegrationHistory.builder()
                        .groupId(streamingRawData.getGroupId())
                        .requestId(streamingRawData.getRequestId())
                        .description(ExceptionUtils.getStackTrace(e))
                        .timestamp(System.currentTimeMillis())
                        .build()
                );
                throw e;
            }
        }
        return transformedObjects;
    }

    List<CommonDataModel> process(StreamingRawData streamingRawData) {
        List<CommonDataModel> transformedObjects = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        List<CommonDataModel> dataSet = new ArrayList<>();
        List<StreamingRawData.TransformerInfoRequest> transformerInfos = streamingRawData.getTransformerInfo();
        for (StreamingRawData.TransformerInfoRequest transformerInfo : transformerInfos) {
            String transformerId = transformerInfo.getTransformerId();
            String entityName = transformerInfo.getEntityName();
            JsonNode jsonNode = streamingRawData.getFeatures().get(0);
            Class<? extends CommonDataModel> entityClass = EntityUtils.get().getEntityClass(transformerInfo.getEntityName());
            CommonDataModelService cdmService = ServiceLocator.lookup(entityClass);
            List<? extends CommonDataModel> cdms = dataTransformationService.transformData(transformerId, entityName, jsonNode);
            for (CommonDataModel tempCdm : cdms) {
                CommonDataModel refresh = cdmService.refresh(tempCdm);
                Optional<String> preprocessValidationExcludeGroup=Optional.ofNullable(transformerInfo.getPreprocessValidationExcludeGroup());
                OperationResponse response = pipelineServiceProcess(refresh, preprocessValidationExcludeGroup);
                if (response.getStatus().equals(OperationResponse.OperationStatus.Failure)) {
                    findErrors(response, errorList);
                } else {
                    List<CommonDataModel> enrichedData = response.getEnrichment().getEnrichedData();
                    dataSet.addAll(enrichedData);
                }
            }
        }
        if (errorList.isEmpty()) {
            try {
               dataSet.parallelStream().forEach(s -> {
                    CommonDataModelService cdmService = ServiceLocator.lookup(s.getClass());
                    cdmService.save(s);
                });
            } catch (Throwable th) {
                log.info("STACKTRACE {}", ExceptionUtils.getStackTrace(th));
                throw th;
            }
        }
        if(ObjectUtils.isNotEmpty(errorList)) {
            throw  new CustomRuntimeException(org.apache.commons.lang3.StringUtils.join(errorList, SEPARATOR));
        }
        return transformedObjects;
    }

    private void findErrors(OperationResponse response, List<String> errorList) {
        if (response.getEnrichment() != null && response.getEnrichment().getStatus().equals(EnrichmentResult.Status.ERROR)) {
            List<EnrichmentResult> enrichmentResult = response.getEnrichment().getEnrichmentResults();
            enrichmentResult.forEach(en -> log.info("{} {}{}", en, en.getStatus(), en.getMessage()));
            String error = org.apache.commons.lang.StringUtils.join(enrichmentResult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(StringUtils.format(ENRICHMENT_ERROR, error));
        }
        if (response.getEntityValidation() != null && response.getEntityValidation().getStatus().equals(ValidationResult.Status.ERROR)) {
            ValidationResult enrichmentResult = response.getEntityValidation();
            List<RuleResult> ruleresult = response.getValidation() != null ? response.getValidation().getViolations() : new ArrayList<>();
            ruleresult.forEach(en -> log.info("status:{}, reason:{}", en.getStatus(), en.getMessage()));
            String error = org.apache.commons.lang.StringUtils.join(ruleresult.stream().map(RuleResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(StringUtils.format(ENTITY_VALIDATION_ERROR, error));
        }
        if (response.getValidation() != null && response.getValidation().getStatus().equals(ValidationResult.Status.ERROR)) {
            List<RuleResult> ruleresult = response.getValidation().getViolations();
            ruleresult.forEach(en -> log.info("status: {}, reason:{}", en.getStatus(), en.getMessage()));
            String error = org.apache.commons.lang.StringUtils.join(ruleresult.stream().map(RuleResult::getMessage).collect(Collectors.toList()), SEPARATOR);
            errorList.add(StringUtils.format(VALIDATION_ERROR, error));
        }
    }

    public OperationResponse pipelineServiceProcess(CommonDataModel refresh, Optional<String> preprocessValidationExcludeGroup) {
        OperationResponse or = new OperationResponse();
        EnrichmentOperationResult enrich = dataEnrichmentService.enrich(refresh, EnrichmentPhase.PRE_VALIDATION);
        or.setEnrichment(enrich);
        List<CommonDataModel> enrichedData = enrich.getEnrichedData();
        boolean cStatus = enrich.getStatus().equals(EnrichmentResult.Status.OK);
        if (cStatus) {
            ValidationResult vr = dataValidationService.validate(enrichedData,preprocessValidationExcludeGroup);
            or.setValidation(vr);
            cStatus = vr.getStatus().equals(ValidationResult.Status.OK);
        }
        if (cStatus) {
            ValidationResult vr = dataEntityValidationService.validate(enrichedData,preprocessValidationExcludeGroup);
            or.setEntityValidation(vr);
            cStatus = vr.getStatus().equals(ValidationResult.Status.OK);
        }
        if (cStatus) {
            EnrichmentOperationResult erPost = dataEnrichmentService.enrich(enrichedData, EnrichmentPhase.POST_VALIDATION);
            or.getEnrichment().merge(erPost);
            cStatus = erPost.getStatus().equals(EnrichmentResult.Status.OK);
        }
        if (cStatus) {
            or.setStatus(OperationResponse.OperationStatus.Success);
        } else {
            or.setStatus(OperationResponse.OperationStatus.Failure);
        }
        return or;
    }
}
