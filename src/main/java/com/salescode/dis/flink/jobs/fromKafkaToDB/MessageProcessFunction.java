package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.ETLPipelineService;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Log4j
public class MessageProcessFunction extends ProcessFunction<List<ObjectNode>, List<CommonDataModel>> {

    private OutputTag<String> deadLetterTag;
    private transient ETLPipelineService etlPipelineService;
    private transient Logger logger;

    transient ExecutorService executorService = Executors.newFixedThreadPool(10);
    public MessageProcessFunction(OutputTag<String> deadLetterTag) {
        this.deadLetterTag = deadLetterTag;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        log.info("Initializing Context");
        log.warn("Initializing Context");
        logger = Logger.getLogger(this.getClass());
        System.setProperty("sun.net.maxDatagramSockets", "2048");
        ConfigurableApplicationContext run = SpringApplication.run(DataIntegrationApplication.class);
        log.info("Context {}" + run);
        etlPipelineService = run.getBean(ETLPipelineService.class);
        log.info("found etlPipelineService {}" + etlPipelineService);
    }

    @Override
    public void processElement(List<ObjectNode> records, Context context, Collector<List<CommonDataModel>> out) throws Exception {
        List<CommonDataModel> processedRecords = new ArrayList<>();
        List<ObjectNode> failedRecords = new ArrayList<>();
        List<Future<List<CommonDataModel>>> futureList = new ArrayList<>();
        try {
            try {
                List<CommonDataModel> result = etlPipelineService.executeBatch(records.stream().map(s->s.toString()).collect(Collectors.toList()));
                if (result != null) {
                    processedRecords.addAll(result);
                }
            } catch (Exception e) {
                logger.error("Failed to process record: ", e);
                failedRecords.addAll(records);
            }
            if (!processedRecords.isEmpty()) {
                out.collect(processedRecords);
            }
            for (ObjectNode failedRecord : failedRecords) {
                failedRecord.put("failure", "Processing failed");
                context.output(deadLetterTag, failedRecord.toString());
            }
        } catch (Exception e) {
            logger.error("Batch processing failed", e);
            records.forEach(record -> {
                record.put("failure", ExceptionUtils.getStackTrace(e));
                context.output(deadLetterTag, record.toString());
            });
        }
    }
}
