package com.salescode.dim;

import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.Collector;
import org.junit.Test;


import java.util.*;

/**
 * A custom sink that collects all elements into a static list for assertions.
 */
class CollectSink<T> implements SinkFunction<T> {
    // Must be static because Flink instantiates the sink in different tasks
    public static final List<Object> values = Collections.synchronizedList(new ArrayList<>());
    private static final long serialVersionUID = -3301416186450647445L;

    public static void clear() {
        values.clear();
    }

    @Override
    public void invoke(T value, Context context) {
        values.add(value);
    }
}

public class DataStreamJobTest {

    @Test
    public void testDataStreamJobWithFewObjects() throws Exception {
        CollectSink.clear();

        Configuration configuration = Configuration.fromMap(Map.of(RestOptions.PORT.key(), "8000"));
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration);

        env.setParallelism(8);


        List<String> entities = List.of("OutletDetails", "User", "Product");

        GeneratorFunction<Long, JsonNode> generatorFunction = index -> {
            int i = index.intValue() % entities.size();
            return JSONUtils.getObjectMapper()
                    .readTree(StringSubstitutor.replace("{\n" +
                            "    \"groupId\": \"%(groupId)\",\n" +
                            "    \"entityName\": \"%(entityName)\"\n" +
                            "}", Map.of("groupId", "req-" + i, "entityName", entities.get(i)), "%(", ")"));
        };

        DataGeneratorSource<JsonNode> outDataGeneratorSource = new DataGeneratorSource<>(
                generatorFunction,
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(JsonNode.class)
        );

        DataStream<JsonNode> source = env.fromSource(outDataGeneratorSource, WatermarkStrategy.noWatermarks(), "Source").setParallelism(1);

        var window = source.keyBy(new KeySelector<JsonNode, String>() {
                    @Override
                    public String getKey(JsonNode streamingRawData) throws Exception {
                        return streamingRawData.get("entityName").asText().hashCode()+"";
                    }
                }).countWindow(3)
                .aggregate(new ListAggregator<>())
                .setParallelism(3)

//
//        var process = window.process(new ProcessWindowFunction<JsonNode, List<JsonNode>, String, TimeWindow>() {
//            @Override
//            public void process(String s, ProcessWindowFunction<JsonNode, List<JsonNode>, String, TimeWindow>.Context context, Iterable<JsonNode> elements, Collector<List<JsonNode>> out) throws Exception {
//                List<JsonNode> list = new ArrayList<>();
//                elements.forEach(list::add);
//                System.out.println("Window processed for key: " + s + " with " + list.size() + " elements " + list);
//                out.collect(list);
//            }
//        })
                .process(new ProcessFunction<List<JsonNode>, JsonNode>() {
                    @Override
                    public void processElement(List<JsonNode> value, ProcessFunction<List<JsonNode>, JsonNode>.Context ctx, Collector<JsonNode> out) throws Exception {
                        System.out.println("Processed key: " + "size : " + value.size() + " elements in a batch : " + value);
                        value.forEach(out::collect);
                    }
                }).setParallelism(6);

        window.addSink(new CollectSink<>()).setParallelism(2);

        env.disableOperatorChaining();
        // Execute the pipeline
        env.execute("DataStreamJob Test");

    }


}
