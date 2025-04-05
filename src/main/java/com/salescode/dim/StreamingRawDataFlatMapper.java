package com.salescode.dim;

import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.util.Collector;

import java.util.List;

class StreamingRawDataFlatMapper extends RichFlatMapFunction<StreamingRawData, StreamingRawData> {
    private static final long serialVersionUID = -3037429674300971140L;

    private static ObjectMapper objectMapper;

    public static void main(String[] args) throws Exception {
        // Create a simple test instance
        StreamingRawDataFlatMapper mapper = new StreamingRawDataFlatMapper();
        mapper.open(new Configuration());

        List<StreamingRawData> outputs = new ArrayList<>();
        Collector<StreamingRawData> collector = new Collector<StreamingRawData>() {
            @Override
            public void collect(StreamingRawData record) {
                outputs.add(record);
                System.out.println("Collected: Features=" + record.getFeatures());
            }

            @Override
            public void close() {
            }
        };

        // Create test input with 2 features
        StreamingRawData input = createSimpleTestData();

        System.out.println("Input: 1 record with " + input.getFeatures().size() + " features");

        // Execute flat mapping
        mapper.flatMap(input, collector);

        // Print summary
        System.out.println("\nResults: " + outputs.size() + " records generated");
        System.out.println("Flat mapping " + (outputs.size() == input.getFeatures().size() ? "SUCCESSFUL" : "FAILED"));
    }

    private static StreamingRawData createSimpleTestData() {
        // Create minimal test objects to demonstrate the flat mapping
        TransformerInfo.TransformerInfoBuilder builder = TransformerInfo.builder();
        TransformerInfo t1 = builder.transformerId("t1").build();
        TransformerInfo t2 = builder.transformerId("t2").build();

        ArrayNode features = objectMapper.createArrayNode();
        features.add(objectMapper.createObjectNode().put("key1", "value1"));
        features.add(objectMapper.createObjectNode().put("key2", "value2"));

        return StreamingRawData.builder().transformerInfo(List.of(t1, t2)) // Transformers remain unchanged
                               .features(features) // Features will be split
                               .build();
    }

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