package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.salescode.dim.jooq.generated.Tables.CK_INTEGRATION_HISTORY;

public class BatchSaveProcessor extends ProcessFunction<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>, StreamingRawData> {

    private static final long serialVersionUID = -8431294998171788516L;
    private final Properties properties;
    private transient Connection connection;
    private transient DSLContext dslContext;
    private transient ServiceLocator serviceLocator;

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
        DatabaseConnectionUtil.initConnectionPool(properties);
        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext();
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
                record.setTimestamp(Instant.now().toEpochMilli());
                records.add(record);
            }

            dslContext.batchInsert(records).execute();
    }

    @Override
    public void processElement(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> value, ProcessFunction<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>, StreamingRawData>.Context ctx, Collector<StreamingRawData> out) throws Exception {
        Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> aggregatedModels = getClassSetMap(value);

        for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : aggregatedModels.entrySet()) {
            CommonDataModelService service = ServiceLocator.lookup(entry.getKey());
            Set<CommonDataModel> models = entry.getValue();

            try {
                service.batchSave(models);

                saveBatchIntegrationHistory(models, "SUCCESS", "Batch save successful");
            } catch (Exception batchEx) {
              //  logger.warn("Batch save failed for " + modelClass.getSimpleName() + ", falling back to individual saves", batchEx);

                // Fallback to individual save for each model
                for (CommonDataModel model : models) {
                    try {
                        service.save(model);
                        saveIntegrationHistory(model, "SUCCESS", "Individual save successful after batch failure");
                    } catch (Exception individualEx) {
                     //   logger.error("Individual save failed for model: " + model.getClass().getSimpleName() + " with ID: " + model.getId(), individualEx);
                        saveIntegrationHistory(model, "FAILURE", "Both batch and individual save failed: " + individualEx.getMessage());
                    }
                }
            }

        }


        value.forEach(tuple -> out.collect(tuple.f0));
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
}
