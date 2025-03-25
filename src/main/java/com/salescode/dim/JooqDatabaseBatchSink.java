package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import com.salescode.dim.utils.EventListenerDTO;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.salescode.dim.jooq.generated.tables.CkIntegrationHistory.CK_INTEGRATION_HISTORY;

public class JooqDatabaseBatchSink implements Sink<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

    private static final long serialVersionUID = 6676299950699299484L;
    private static final Logger LOG = LoggerFactory.getLogger(JooqDatabaseBatchSink.class);
    private final Properties properties;
    private final int batchSize;
    private final long batchIntervalMs;


    public JooqDatabaseBatchSink(Properties dbProperties) {
        this.properties = dbProperties;
        this.batchSize = Integer.parseInt(dbProperties.getProperty("batch.size", "500"));
        this.batchIntervalMs = Long.parseLong(dbProperties.getProperty("batch.interval.ms", "20000"));
    }

    @Override
    public SinkWriter<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> createWriter(InitContext context) {
        try {
            return new JooqDatabaseBatchSinkWriter(properties, batchSize, batchIntervalMs);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Error initializing JooqDatabaseBatchSink", e);
        }
    }

    private static class JooqDatabaseBatchSinkWriter implements SinkWriter<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

        private final DSLContext dslContext;
        private final List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> batchBuffer;
        private final int batchSize;
        private final long batchIntervalMs;
        private long lastBatchTime;
        private transient ServiceLocator serviceLocator;
        private final KafkaProducer<String, EventListenerDTO> producer;
        private final String topicName;

        public JooqDatabaseBatchSinkWriter(Properties properties, int batchSize, long batchIntervalMs) throws SQLException, ClassNotFoundException {
            HikariDataSource hikariDataSource = DatabaseConnectionUtil.initConnectionPool(properties, 10);
            this.dslContext = DatabaseConnectionUtil.createPooledDSLContext(hikariDataSource);
            this.batchBuffer = new ArrayList<>();
            this.batchSize = batchSize;
            this.batchIntervalMs = batchIntervalMs;
            this.lastBatchTime = System.currentTimeMillis();
            this.serviceLocator = ServiceLocator.getInstance(dslContext);
            serviceLocator.registerSubClasses();

            Properties kafkaProps = new Properties();
            kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventListenerDTOSerializer.class.getName());


            this.producer = new KafkaProducer<>(kafkaProps);
            this.topicName = properties.getProperty("event.topic") + "-" + properties.getProperty("lob");

        }

        @Override
        public void write(Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> value, Context context) throws IOException {
            LOG.info("Write method called with value: {}", value);
            try {
                batchBuffer.add(value);
                long currentTime = System.currentTimeMillis();
                if (batchBuffer.size() >= batchSize) {
                    flush(false);
                    lastBatchTime = currentTime;
                }
            } catch (Exception e) {
                throw new IOException("Failed to add record to batch", e);
            }
        }

        @Override
        public void flush(boolean endOfInput) throws IOException {
            LOG.info("Flushing {} records into sink...", batchBuffer.size());
            if (!batchBuffer.isEmpty()) {
                try {
                    Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> consolidatedModels = new HashMap<>();
                    Map<CommonDataModel, StreamingRawData> modelToRawDataMap = new HashMap<>();
                    for (Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> tuple : batchBuffer) {
                        StreamingRawData data = tuple.f0;
                        for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : tuple.f1.entrySet()) {
                            consolidatedModels.putIfAbsent(entry.getKey(), new HashSet<>());
                            consolidatedModels.get(entry.getKey()).addAll(entry.getValue());
                            for(CommonDataModel cdm : entry.getValue()){
                                modelToRawDataMap.put(cdm, data);
                            }
                        }
                    }

                    for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : consolidatedModels.entrySet()) {
                        CommonDataModelService service = ServiceLocator.lookup(entry.getKey());
                        try {
                            service.batchSave(entry.getValue());
                            for (CommonDataModel model : entry.getValue()) {
                                LOG.info("Operation performed is " + model.getOperationPerformed());
                                if (model.getOperationPerformed() != null) {
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    EventListenerDTO dto = new EventListenerDTO(
                                            rawData.getRequestId(),
                                            entry.getKey().getSimpleName(),
                                            rawData.getLob(),
                                            model.getChanges(),
                                            model.getOperationPerformed()
                                    );

                                    try {
                                        // Convert DTO to JSON

                                        // Publish to Kafka
                                        ProducerRecord<String, EventListenerDTO> record = new ProducerRecord<>(topicName, dto.getRequestId(), dto);
                                        producer.send(record, (metadata, exception) -> {
                                            if (exception != null) {
                                                LOG.error("Failed to publish message to Kafka", exception);
                                            } else {
                                                LOG.info("Published message to Kafka topic: {} at offset {}", metadata.topic(), metadata.offset());
                                            }
                                        });
                                    } catch (Exception ex) {
                                        LOG.error("Error serializing EventListenerDTO for Kafka", ex);
                                    }
                                }
                            }
                            saveBatchIntegrationHistory(entry.getValue(), "SUCCESS", "Batch save successful");
                        } catch (Exception batchEx) {
                            LOG.error("Batch save failed. Falling back to individual saves.");
                            for (CommonDataModel model : entry.getValue()) {
                                try {
                                    service.batchSave(List.of(model));
                                    saveIntegrationHistory(model, "SUCCESS", "Individual save successful");
                                } catch (Exception individualEx) {
                                    saveIntegrationHistory(model, "FAILURE", "Save failed: " + individualEx.getMessage());
                                }
                            }
                        }
                    }
                    batchBuffer.clear();
                } catch (Exception e) {
                    throw new IOException("Batch processing failed", e);
                }
            }
        }

        private void saveIntegrationHistory(CommonDataModel model, String status, String message) {
            CkIntegrationHistoryRecord record = new CkIntegrationHistoryRecord();
            record.setId(UUID.randomUUID().toString());
            record.setStatus(status);
            record.setDescription(message);
            record.setTimestamp(Instant.now().toEpochMilli());
            dslContext.insertInto(CK_INTEGRATION_HISTORY).set(record).execute();
        }

        private void saveBatchIntegrationHistory(Set<CommonDataModel> models, String status, String message) {
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
        }

        @Override
        public void close() throws Exception {
            flush(true);
//            if (connection != null) {
//                connection.close();
//            }
            LOG.info("Closed connection successfully");
        }
    }
}