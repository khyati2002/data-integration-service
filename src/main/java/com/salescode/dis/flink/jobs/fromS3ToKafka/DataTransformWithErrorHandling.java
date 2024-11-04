package com.salescode.dis.flink.jobs.fromS3ToKafka;

import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class DataTransformWithErrorHandling extends ProcessFunction<String, String> {
    private final OutputTag<String> deadLetterTag;
    // private static final Logger LOGGER = LoggerFactory.getLogger(DataTransformWithErrorHandling.class);

    public DataTransformWithErrorHandling(OutputTag<String> deadLetterTag) {
        this.deadLetterTag = deadLetterTag;
    }

    @Override
    public void processElement(String value, Context ctx, Collector<String> out) {
        // LOGGER.debug("Processing element: {}", value);
        try {
            // Perform transformation (this could throw an exception)
            String transformedValue = transform(value);
            out.collect(transformedValue);
        } catch (Exception e) {
            // If an error occurs, send the value to the dead-letter side output
            // LOGGER.error("Error processing element: {}. Sending to dead letter topic.", value);
            ctx.output(deadLetterTag, value);
        }
    }

    private String transform(String value) {
        // LOGGER.debug("Transforming value: {}", value);
        // Placeholder for transformation logic (may throw an exception)
        return value;
    }
}