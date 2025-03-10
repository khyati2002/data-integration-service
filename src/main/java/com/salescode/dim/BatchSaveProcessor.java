package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BatchSaveProcessor extends ProcessFunction<CommonDataModel, CommonDataModel> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchSaveProcessor.class);
    private static final int BATCH_SIZE = 1;
    private static final long FLUSH_INTERVAL_MS = 2000; // Change batch size as needed

    private final Properties properties;

    // Map to store batches separately for each CommonDataModel type
    private final Map<Class<?>, List<CommonDataModel>> batchMap = new HashMap<>();
    private final Map<Class<?>, Long> lastProcessedTimeMap = new HashMap<>();

    public BatchSaveProcessor(Properties commonProperties) {
        this.properties = commonProperties;
    }

    @Override
    public void processElement(CommonDataModel value, Context ctx, Collector<CommonDataModel> out) throws Exception {
        Class<?> type = value.getClass();

        batchMap.putIfAbsent(type, new ArrayList<>());
        lastProcessedTimeMap.putIfAbsent(type, System.currentTimeMillis());

        List<CommonDataModel> batch = batchMap.get(type);
        batch.add(value); // Add to batch

        // Always update last processed time when a new record is added
        lastProcessedTimeMap.put(type, System.currentTimeMillis());

        // If batch reaches size, process it immediately
        if (batch.size() >= BATCH_SIZE) {
            processBatch(type, batch, out);
            lastProcessedTimeMap.put(type, System.currentTimeMillis()); // Reset last processed time
        } else {
            // Register a timer to flush batch after FLUSH_INTERVAL_MS
            ctx.timerService().registerProcessingTimeTimer(
                    lastProcessedTimeMap.get(type) + FLUSH_INTERVAL_MS
            );
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<CommonDataModel> out) {
        for (Map.Entry<Class<?>, List<CommonDataModel>> entry : batchMap.entrySet()) {
            Class<?> type = entry.getKey();
            List<CommonDataModel> batch = entry.getValue();

            if (!batch.isEmpty()) {
                LOG.info("Flushing batch via timer for type: {}", type.getSimpleName());
                processBatch(type, batch, out);
                batch.clear();  // Ensure batch is cleared after processing
                lastProcessedTimeMap.put(type, System.currentTimeMillis()); // Reset last processed time
            }
        }
    }


    private void processBatch(Class<?> type, List<CommonDataModel> batch, Collector<CommonDataModel> out) {
        if (batch.isEmpty()) {
            return;
        }

        try {
            LOG.info("Saving batch of size: {} for type: {}", batch.size(), type.getSimpleName());
            CommonDataModelService cdmService = ServiceLocator.lookup(batch.get(0).getClass());
            List<CommonDataModel> savedBatch = cdmService.batchSave(batch);
            LOG.info("Batch save success for type: {}", type.getSimpleName());

            savedBatch.forEach(data -> {
                out.collect(data);
            });

            batch.clear(); // Clear batch after processing
        } catch (Exception e) {
            LOG.error("Batch save failed for type: {}. Falling back to individual save.", type.getSimpleName(), e);

            // Attempt individual save
            for (CommonDataModel cdm : batch) {
                try {
                    CommonDataModelService cdmService = ServiceLocator.lookup(cdm.getClass());
                    out.collect((CommonDataModel) cdmService.save(cdm));
                } catch (Exception ex) {
                    LOG.error("Failed to save individual record: {}", cdm, ex);
                }
            }

            batch.clear();  // Clear batch after fallback processing
        }
    }

    @Override
    public void close() {
        for (Map.Entry<Class<?>, List<CommonDataModel>> entry : batchMap.entrySet()) {
            Class<?> type = entry.getKey();
            List<CommonDataModel> batch = entry.getValue();

            if (!batch.isEmpty()) {
                LOG.info("Final flush in close() for type: {}", type.getSimpleName());
                processBatch(type, batch, new Collector<>() {
                    @Override
                    public void collect(CommonDataModel record) {
                        LOG.info("Final batch processed for type: {}", type.getSimpleName());
                    }

                    @Override
                    public void close() {
                    }
                });
            }
        }
    }
}
