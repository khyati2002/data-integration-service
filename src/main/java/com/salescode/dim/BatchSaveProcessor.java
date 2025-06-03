package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.salescode.dim.jooq.generated.Tables.CK_INTEGRATION_HISTORY;

public class BatchSaveProcessor extends ProcessAllWindowFunction<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>, EventListenerDTO, GlobalWindow> {

    private static final long serialVersionUID = -8431294998171788516L;
    private final Properties properties;
    private transient Connection connection;
    private transient DSLContext dslContext;
    private transient ServiceLocator serviceLocator;

    private static final Logger LOG = LoggerFactory.getLogger(BatchSaveProcessor.class);
    public BatchSaveProcessor(Properties commonProperties) {
        this.properties = commonProperties;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
//        this.connection = DatabaseConnectionUtil.createConnection(properties);
//        this.dslContext = DatabaseConnectionUtil.createDSLContext(connection);
        initializeResources();
        this.serviceLocator = ServiceLocator.getInstance(dslContext);
        serviceLocator.registerSubClasses();
    }

    private void initializeResources () throws SQLException {
//        DatabaseConnectionUtil.initConnectionPool(properties);
//        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext();
    }

    private void saveIntegrationHistory(CommonDataModel model, String status, String message) {
        CkIntegrationHistoryRecord record = new CkIntegrationHistoryRecord();
        record.setId(UUID.randomUUID().toString());
        record.setStatus(status);
        record.setDescription(message);
        record.setTimestamp(Instant.now().toEpochMilli());

        dslContext.insertInto(CK_INTEGRATION_HISTORY)
                .set(record)
                .execute();
    }

    private void saveBatchIntegrationHistory(Set<CommonDataModel> models, String status, String message) {

            // Create a batch of integration history records
            List<CkIntegrationHistoryRecord> records = new ArrayList<>();
            long currentTimestamp = Instant.now().toEpochMilli();

            for (CommonDataModel model : models) {
                CkIntegrationHistoryRecord record = new CkIntegrationHistoryRecord();
                record.setId(UUID.randomUUID().toString());
                record.setStatus(status);
                record.setDescription(message);
                record.setTimestamp(currentTimestamp);
                records.add(record);
            }

            dslContext.batchInsert(records).execute();

            LOG.info("Records Saved successfully");
    }

    public void processElement(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> value1,
                               Collector<EventListenerDTO> out) throws Exception {

        List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> value = new ArrayList<>(value1);
        LOG.info("ListAggregator aggregated size: " + value.size());

        // Group all models by their class type across all tuples
        Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> consolidatedModels = new HashMap<>();
        Map<CommonDataModel, StreamingRawData> modelToRawDataMap = new HashMap<>();

        // First pass - collect all models by type
        for (Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> tuple : value) {
            StreamingRawData rawData = tuple.f0;

            for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : tuple.f1.entrySet()) {
                Class<? extends CommonDataModel> modelClass = entry.getKey();
                Set<CommonDataModel> models = entry.getValue();

                // Create the set if this is the first time we're seeing this model class
                consolidatedModels.putIfAbsent(modelClass, new HashSet<>());

                // Add all models to the consolidated set
                for (CommonDataModel model : models) {
                    consolidatedModels.get(modelClass).add(model);
                    modelToRawDataMap.put(model, rawData);  // Remember which raw data this model came from
                }
            }
        }

            // Now process each model type with all models together
        for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : consolidatedModels.entrySet()) {
            Class<? extends CommonDataModel> modelClass = entry.getKey();
            Set<CommonDataModel> allModels = entry.getValue();

            LOG.info("Processing consolidated batch of " + modelClass.getSimpleName() +
                    " models. Total count: " + allModels.size());

            CommonDataModelService service = ServiceLocator.lookup(modelClass);

            try {
                // Process all models of this type in a single batch operation
                LOG.info("Batch save called");
                service.batchSave(allModels);
                LOG.info("ListAggregator aggregated size is   : " + value.size());
                LOG.info("Batch save success");
//                saveBatchIntegrationHistory(allModels, "SUCCESS", "Consolidated batch save successful");

                // Collect DTOs for each model
                for (CommonDataModel model : allModels) {
                    if (model.getOperationPerformed() != null) {
                        StreamingRawData rawData = modelToRawDataMap.get(model);
                        EventListenerDTO dto = new EventListenerDTO(
                                rawData.getRequestId(),
                                modelClass.getSimpleName(),
                                rawData.getLob(),
                                model.getChanges(),
                                model.getOperationPerformed(),
                                model.getId()
                        );
                        out.collect(dto);
                    }
                }
            } catch (Exception batchEx) {
                LOG.error("Batch save failed for " + modelClass.getSimpleName() +
                        " with error: " + batchEx.getMessage() +
                        ". Falling back to individual saves.");

                // Fall back to individual saves
                for (CommonDataModel model : allModels) {
                    try {
                        service.save(model);
//                        saveIntegrationHistory(model, "SUCCESS", "Individual save successful after batch failure");

                        if (model.getOperationPerformed() != null) {
                            StreamingRawData rawData = modelToRawDataMap.get(model);
                            EventListenerDTO dto = new EventListenerDTO(
                                    rawData.getRequestId(),
                                    modelClass.getSimpleName(),
                                    rawData.getLob(),
                                    model.getChanges(),
                                    model.getOperationPerformed(),
                                    model.getId()
                            );
                            out.collect(dto);
                        }
                    } catch (Exception individualEx) {
//                        saveIntegrationHistory(model, "FAILURE", "Both batch and individual save failed: " + individualEx.getMessage());
                    }
                }
            }
        }
    }

    private static Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> getClassSetMap(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> value) {
        Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> aggregatedModels = new HashMap<>();

        for (Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> tuple : value) {
            Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataModelMap = tuple.f1;

            dataModelMap.forEach((modelClass, models) -> aggregatedModels.computeIfAbsent(modelClass, k -> new HashSet<>())
                    .addAll(models));
        }
        return aggregatedModels;
    }


    @Override
    public void process(ProcessAllWindowFunction<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>, EventListenerDTO, GlobalWindow>.Context context,
                        Iterable<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> elements,
                        Collector<EventListenerDTO> out) throws Exception {
        ArrayList list = new ArrayList();
        elements.forEach(list::add);
        processElement(list,out);
    }
}
