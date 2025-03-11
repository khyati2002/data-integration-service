package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_INTEGRATION_HISTORY;

public class BatchSaveProcessor extends ProcessFunction<CommonDataModel, CommonDataModel> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchSaveProcessor.class);
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 5000; // Change batch size as needed

    private final Properties properties;
    // Map to store batches separately for each CommonDataModel type
    private final Map<Class<?>, List<CommonDataModel>> batchMap = new HashMap<>();
    private final Map<Class<?>, Long> lastProcessedTimeMap = new HashMap<>();
    private transient Connection connection;
    private transient DSLContext dslContext;
    public BatchSaveProcessor(Properties commonProperties) {
        this.properties = commonProperties;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        LOG.info("BatchSaveProcessor.open() called");
        super.open(parameters);
        initializeResources();
    }

    private void initializeResources() throws Exception {
        // Create connection & DSLContext using the utility
        LOG.info("Initialize resources in BatchSaveProcessor.open() called");
        DatabaseConnectionUtil.initConnectionPool(properties);
        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext();
    }

    @Override
    public void processElement(CommonDataModel value, Context ctx, Collector<CommonDataModel> out) throws Exception {
        LOG.info("Batch Processor Called");
        Class<?> type = value.getClass();

        batchMap.putIfAbsent(type, new ArrayList<>());
        lastProcessedTimeMap.putIfAbsent(type, System.currentTimeMillis());

        List<CommonDataModel> batch = batchMap.get(type);
        batch.add(value); // Add to batch

        // If batch reaches size or if too much time has passed, process it immediately
        if (batch.size() >= BATCH_SIZE ||
                (System.currentTimeMillis() - lastProcessedTimeMap.get(type) > FLUSH_INTERVAL_MS)) {
            processBatch(type, batch, out);
            lastProcessedTimeMap.put(type, System.currentTimeMillis()); // Reset last processed time
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
            saveBatchSaveSuccess(savedBatch);
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
                    CkIntegrationHistoryRecord record = dslContext.newRecord(CK_INTEGRATION_HISTORY);
                    record.setId(UUID.randomUUID().toString());
                    record.setEntityName(cdm.getClass().getSimpleName());
                    record.setStatus("Success");
                    record.setDescription("Success");
                    record.setTimestamp(Instant.now().toEpochMilli());
                    dslContext.insertInto(CK_INTEGRATION_HISTORY)
                            .set(record)
                            .execute();
                } catch (Exception ex) {
                    LOG.error("Failed to save individual record: {}", cdm, ex);
                    CkIntegrationHistoryRecord record = dslContext.newRecord(CK_INTEGRATION_HISTORY);
                    record.setId(UUID.randomUUID().toString());
                    record.setEntityName(cdm.getClass().getSimpleName());
                    record.setStatus("Failure");
                    record.setDescription(String.valueOf(ex));
                    record.setTimestamp(Instant.now().toEpochMilli());
                    dslContext.insertInto(CK_INTEGRATION_HISTORY)
                            .set(record)
                            .execute();
                }
            }

            batch.clear();  // Clear batch after fallback processing
        }
    }

    private void saveBatchSaveSuccess(List<CommonDataModel> savedbatch) {
        List<CkIntegrationHistoryRecord> records = savedbatch.stream().map(cdm -> {
            CkIntegrationHistoryRecord record = dslContext.newRecord(CK_INTEGRATION_HISTORY);
            record.setId(UUID.randomUUID().toString());
            record.setEntityName(cdm.getClass().getSimpleName());
            record.setStatus("Success");
            record.setDescription("Success");
            record.setTimestamp(Instant.now().toEpochMilli());
            return record;
        }).collect(Collectors.toList());

        if (!records.isEmpty()) {
            dslContext.batchInsert(records).execute();
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
