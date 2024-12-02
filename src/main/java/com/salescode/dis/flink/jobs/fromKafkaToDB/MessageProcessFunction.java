package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.ETLPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@Slf4j
public class MessageProcessFunction extends KeyedProcessFunction<String, Tuple2<String, ObjectNode>, CommonDataModel> {

    OutputTag<String> deadLetterTag;
    private transient ETLPipelineService etlPipelineService;

    public MessageProcessFunction(OutputTag<String> deadLetterTag){
        this.deadLetterTag = deadLetterTag;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        log.info("Initializating Context");
        ConfigurableApplicationContext run = SpringApplication.run(DataIntegrationApplication.class);
        log.info("Context {}", run);
        etlPipelineService = run.getBean(ETLPipelineService.class);
        log.info("found etlPipelineService {}", etlPipelineService);
    }

    @Override
    public void processElement(Tuple2<String, ObjectNode> tuple, Context context, Collector<CommonDataModel> out) throws Exception {
        try {
            System.out.println("Tuple key:"+tuple.f0);
            System.out.println("Tuple node:"+tuple.f1);
            // Process the JsonNode here before sinking it
            List<CommonDataModel> record = processJsonNode(tuple.f1);
            // Emit the processed tuple
            record.forEach(out::collect);
        } catch (Exception e) {
            e.printStackTrace();
            // context.output(deadLetterTag, tuple.f1.asText());
            throw e;
            log.info("Exception Stacktrace {}",e);
             context.output(deadLetterTag, tuple.f1.toString().concat("====").concat(e.getMessage()));
        }
    }

    // Custom method to process the JsonNode before sinking to the database
    private List<CommonDataModel> processJsonNode(ObjectNode jsonNode) {
        // Add your processing logic here (e.g., modifying fields, filtering, transforming data)
        // For example, modifying a field or adding a new field
        // return jsonNode;
        List<CommonDataModel> execute = etlPipelineService.execute(jsonNode.toString());
        return execute;
        //Do JSONNode to Record mapping here

    }
}