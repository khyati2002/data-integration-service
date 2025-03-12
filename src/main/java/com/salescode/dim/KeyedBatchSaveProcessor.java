package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.salescode.dim.jooq.generated.Tables.CK_INTEGRATION_HISTORY;

public class KeyedBatchSaveProcessor extends KeyedProcessFunction<String, CommonDataModel, CommonDataModel> {
    private static final Logger LOG = LoggerFactory.getLogger(KeyedBatchSaveProcessor.class);
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 10_000L;

    private final Properties properties;
    private transient DSLContext dslContext;
    private transient ListState<CommonDataModel> batchState;
    private transient ValueState<Long> lastProcessedTime;
    public KeyedBatchSaveProcessor(Properties commonProperties) {
        this.properties = commonProperties;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        initializeResources();
        batchState = getRuntimeContext().getListState(new ListStateDescriptor<>("batchState", CommonDataModel.class));
        lastProcessedTime = getRuntimeContext().getState(new ValueStateDescriptor<>("lastProcessedTime", Long.class));

    }

    private void initializeResources() throws Exception {
        // Create connection & DSLContext using the utility
        LOG.info("Initialize resources in BatchSaveProcessor.open() called");
        DatabaseConnectionUtil.initConnectionPool(properties);
        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext();
    }

    @Override
    public void processElement(CommonDataModel value, Context ctx, Collector<CommonDataModel> out) throws Exception {
        // 🚀 Initialize batchState safely
        List<CommonDataModel> batch = new ArrayList<>();
        if (batchState.get() != null) {
            for (CommonDataModel cdm : batchState.get()) {
                batch.add(cdm);
            }
        }

        // ✅ Add the new record
        batch.add(value);

        // 🚀 Initialize lastProcessedTime safely
        Long lastTime = lastProcessedTime.value();
        if (lastTime == null) {
            lastTime = System.currentTimeMillis();
            lastProcessedTime.update(lastTime);
        }

        if (batch.size() >= BATCH_SIZE) {
            processBatch(batch, out);
            batchState.clear(); // ✅ Clear after processing
            lastProcessedTime.update(System.currentTimeMillis()); // Reset timer
            ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + FLUSH_INTERVAL_MS);
        } else {
            batchState.update(batch); // ✅ Ensure batch is updated properly
            ctx.timerService().registerProcessingTimeTimer(lastTime + FLUSH_INTERVAL_MS);
        }
    }



    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<CommonDataModel> out) throws Exception {
        List<CommonDataModel> batch = new ArrayList<>();
        for (CommonDataModel cdm : batchState.get()) {
            batch.add(cdm);
        }
        if (!batch.isEmpty()) {
            processBatch(batch, out);
        }
    }

    private void processBatch(List<CommonDataModel> batch, Collector<CommonDataModel> out) {
        if (batch.isEmpty()) return;

        try {
            LOG.info("Saving batch of size: {}", batch.size());
            CommonDataModelService cdmService = ServiceLocator.lookup(batch.get(0).getClass());
            List<CommonDataModel> savedBatch = cdmService.batchSave(batch);
            saveBatchSaveSuccess(savedBatch);
            savedBatch.forEach(out::collect);
            batchState.clear();
        } catch (Exception e) {
            LOG.error("Batch save failed. Falling back to individual save.", e);
            for (CommonDataModel cdm : batch) {
                try {
                    CommonDataModelService cdmService = ServiceLocator.lookup(cdm.getClass());
                    out.collect(cdmService.save(cdm));
                    saveIntegrationHistory(cdm, "Success", "Saved successfully");
                    batchState.clear();
                    return;
                } catch (Exception ex) {
                    LOG.error("Failed to save individual record: {}", cdm, ex);
                    saveIntegrationHistory(cdm, "Failure", ex.getMessage());
                }
            }
        }
    }

    private void saveBatchSaveSuccess(List<CommonDataModel> savedBatch) {
        List<CkIntegrationHistoryRecord> records = new ArrayList<>();
        for (CommonDataModel cdm : savedBatch) {
            CkIntegrationHistoryRecord record = new CkIntegrationHistoryRecord();
            record.setId(UUID.randomUUID().toString());
            record.setEntityName(cdm.getClass().getSimpleName());
            record.setStatus("Success");
            record.setDescription("Success");
            record.setTimestamp(Instant.now().toEpochMilli());
            records.add(record);
        }
        if (!records.isEmpty()) {
            dslContext.batchInsert(records).execute();
        }
    }

    private void saveIntegrationHistory(CommonDataModel cdm, String status, String description) {
        CkIntegrationHistoryRecord record = dslContext.newRecord(CK_INTEGRATION_HISTORY);
        record.setId(UUID.randomUUID().toString());
        record.setEntityName(cdm.getClass().getSimpleName());
        record.setStatus(status);
        record.setDescription(description);
        record.setTimestamp(Instant.now().toEpochMilli());
        dslContext.insertInto(CK_INTEGRATION_HISTORY).set(record).execute();
    }
}
