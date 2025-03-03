package com.salescode.dim;

import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;

class StreamingRawDataFlatMapper extends RichFlatMapFunction<StreamingRawData, StreamingRawData> {
    private static final long serialVersionUID = -3037429674300971140L;

    private static ObjectMapper objectMapper;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        objectMapper = JSONUtils.getObjectMapper();
    }

    @Override
    public void flatMap(StreamingRawData value, Collector<StreamingRawData> out) throws Exception {
        // Iterate over each feature and create a new StreamingRawData object
        for (var feature : value.getFeatures()) {
            // Wrap the single feature in an array
            ArrayNode singleFeatureArray = objectMapper.createArrayNode().add(feature);

            // Create a new StreamingRawData object with only one feature
            StreamingRawData result = value.toBuilder().features(singleFeatureArray) // Change only features
                                           .build();

            out.collect(result);
        }
    }
}