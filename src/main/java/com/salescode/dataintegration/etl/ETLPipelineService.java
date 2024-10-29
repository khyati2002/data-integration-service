package com.salescode.dataintegration.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.dto.StreamingRawData;
import com.salescode.dataintegration.etl.transformer.service.DataTransformationService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ETLPipelineService {

    private final DataTransformationService dataTransformationService;
    ObjectMapper objectMapper = new ObjectMapper();

    public ETLPipelineService(DataTransformationService dataTransformationService) {
        this.dataTransformationService = dataTransformationService;
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
            JsonNode jsonNode = streamingRawData.getFeatures().get(0); // Only one feature per execution
            List<CommonDataModel> objects = dataTransformationService.transformData(transformerId, entityName, jsonNode);
            transformedObjects.addAll(objects);
        }
        return transformedObjects;
    }
}
