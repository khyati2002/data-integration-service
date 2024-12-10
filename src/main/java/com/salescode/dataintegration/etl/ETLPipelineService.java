package com.salescode.dataintegration.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.enums.EnrichmentPhase;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.response.OperationResponse;
import com.salescode.channelkart.services.IntegrationHistoryService;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.dataintegration.etl.dto.StreamingRawData;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dataintegration.etl.transformer.service.DataTransformationService;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.service.DataEntityValidationService;
import com.salescode.dataintegration.etl.validation.service.DataValidationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLPipelineService {

    private final DataTransformationService dataTransformationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final DataValidationService dataValidationService;
    private final DataEntityValidationService dataEntityValidationService;
    private final IntegrationHistoryService integrationHistoryService;
    ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    public ETLPipelineService(DataTransformationService dataTransformationService, DataEnrichmentService dataEnrichmentService, DataValidationService dataValidationService, DataEntityValidationService dataEntityValidationService,IntegrationHistoryService integrationHistoryService) {
        this.dataTransformationService = dataTransformationService;
        this.dataEnrichmentService = dataEnrichmentService;
        this.dataValidationService = dataValidationService;
        this.dataEntityValidationService = dataEntityValidationService;
        this.integrationHistoryService = integrationHistoryService;
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

            try {
                streamingRawData.setFeatures(objectMapper.createArrayNode().add(jsonNode));
                transformedObjects.addAll(process(streamingRawData));
                integrationHistoryService.save("Success","Success");
            }
            catch(Exception e){
                integrationHistoryService.save("Failure",e.getMessage());
            }

        }
        return transformedObjects;
    }

    List<CommonDataModel> process(StreamingRawData streamingRawData) {
        List<CommonDataModel> transformedObjects = new ArrayList<>();
        List<StreamingRawData.TransformerInfoRequest> transformerInfos = streamingRawData.getTransformerInfo();
        for (StreamingRawData.TransformerInfoRequest transformerInfo : transformerInfos) {
            String transformerId = transformerInfo.getTransformerId();
            String entityName = transformerInfo.getEntityName();
            JsonNode jsonNode = streamingRawData.getFeatures().get(0);
            Class<? extends CommonDataModel> entityClass = EntityUtils.getInstance().getEntityClass(transformerInfo.getEntityName());
            CommonDataModelService cdmService = ServiceLocator.lookup(entityClass);
            List<? extends CommonDataModel> cdms = dataTransformationService.transformData(transformerId, entityName, jsonNode);
            log.info("CDM : {}", JSONUtils.getObjectMapper().convertValue(cdms, JsonNode.class).toPrettyString());
            for (CommonDataModel tempCdm : cdms) {
//                String id = cdmService.getKey(tempCdm);
//                if (id == null) {
//                    id = UUID.randomUUID().toString();
//                }
                CommonDataModel refresh = cdmService.refresh(tempCdm);
                OperationResponse or = new OperationResponse();
                EnrichmentOperationResult enrich = dataEnrichmentService.enrich(refresh, EnrichmentPhase.PRE_VALIDATION);
                or.setEnrichment(enrich);
                List<CommonDataModel> enrichedData = enrich.getEnrichedData();
                boolean cStatus = enrich.getStatus().equals(EnrichmentResult.Status.OK);
                if (cStatus) {
                    ValidationResult vr = dataValidationService.validate(enrichedData);
                    or.setValidation(vr);
                    cStatus = vr.getStatus().equals(ValidationResult.Status.OK);
                }
                if (cStatus) {
                    ValidationResult vr = dataEntityValidationService.validate(enrichedData);
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
                transformedObjects.addAll(or.getEnrichment().getEnrichedData().parallelStream().map(s -> cdmService.save(s)).collect(Collectors.toList()));
            }
        }
        return transformedObjects;
    }
//    public List<CommonDataModel> transformData(StreamingRawData streamingRawData) {
//        List<StreamingRawData.TransformerInfoRequest> transformerInfos = streamingRawData.getTransformerInfo();
//        ArrayNode features = streamingRawData.getFeatures();
//        return StreamSupport.stream(features.spliterator(), true)
//                .flatMap(feature -> applyTransformersToFeature(transformerInfos, feature).stream())
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Applies each transformer in the list to a single feature and returns a list of transformed CommonDataModel objects.
//     *
//     * @param transformerInfos List of transformer configurations
//     * @param feature          The feature (JsonNode) to transform
//     * @return List of transformed CommonDataModel objects for the given feature
//     */
//    private List<CommonDataModel> applyTransformersToFeature(List<StreamingRawData.TransformerInfoRequest> transformerInfos, JsonNode feature) {
//        return transformerInfos.stream()
//                .flatMap(transformerInfo -> dataTransformationService.transformData(transformerInfo.getTransformerId(), transformerInfo.getEntityName(), feature).stream())
//                .collect(Collectors.toList());
//    }
}
