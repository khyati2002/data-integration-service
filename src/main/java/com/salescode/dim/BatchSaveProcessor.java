package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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

    @Override
    public void processElement(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> value, ProcessFunction<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>, StreamingRawData>.Context ctx, Collector<StreamingRawData> out) throws Exception {
        Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> aggregatedModels = getClassSetMap(value);

        for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : aggregatedModels.entrySet()) {
            CommonDataModelService service = ServiceLocator.lookup(entry.getKey());
            service.batchSave(entry.getValue());
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
