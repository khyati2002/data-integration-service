package com.salescode.dataintegration.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.cdm.CommonDataModelService;
import com.salescode.dataintegration.etl.cdm.util.ServiceLocator;
import com.salescode.dataintegration.etl.dto.StreamingRawData;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dataintegration.etl.transformer.service.DataTransformationService;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.dataintegration.etl.validation.service.DataEntityValidationService;
import com.salescode.dataintegration.etl.validation.service.DataValidationService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ETLPipelineService {

    private final DataTransformationService dataTransformationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final DataValidationService dataValidationService;
    private final DataEntityValidationService dataEntityValidationService;
    ObjectMapper objectMapper = JSONUtils.getObjectMapper();

    public ETLPipelineService(DataTransformationService dataTransformationService, DataEnrichmentService dataEnrichmentService, DataValidationService dataValidationService, DataEntityValidationService dataEntityValidationService) {
        this.dataTransformationService = dataTransformationService;
        this.dataEnrichmentService = dataEnrichmentService;
        this.dataValidationService = dataValidationService;
        this.dataEntityValidationService = dataEntityValidationService;
    }

    @SneakyThrows
    void execute(String message) {
        StreamingRawData streamingRawData = objectMapper.readValue(message, StreamingRawData.class);
        ArrayNode features = streamingRawData.getFeatures();
        if (features.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be empty");
        }
        for (JsonNode jsonNode : streamingRawData.getFeatures()) {
            streamingRawData.setFeatures(objectMapper.createArrayNode().add(jsonNode));
            List<CommonDataModel> process = process(streamingRawData);
        }
        streamingRawData.setFeatures(features);
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
            for (CommonDataModel tempCdm : cdms) {
                String id = cdmService.getKey(tempCdm);
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
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
