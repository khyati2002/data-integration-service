package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.event.EventPublisher;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.utils.EventListenerDTO;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.util.concurrent.Executors;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.salescode.dim.jooq.generated.tables.CkIntegrationHistory.CK_INTEGRATION_HISTORY;

public class JooqDatabaseBatchSink implements Sink<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

    private static final long serialVersionUID = 6676299950699299484L;
    private static final Logger LOG = LoggerFactory.getLogger(JooqDatabaseBatchSink.class);
    private static MailboxExecutor mailboxExecutor;
    private final Properties properties;
    private final int batchSize;
    private final long batchIntervalMs;



    public JooqDatabaseBatchSink(Properties dbProperties) {
        this.properties = dbProperties;
        this.batchSize = Integer.parseInt(dbProperties.getProperty("batch.size", "5"));
        this.batchIntervalMs = Long.parseLong(dbProperties.getProperty("batch.interval.ms", "20000"));
    }

    @Override
    public SinkWriter<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> createWriter(InitContext context) {
        try {
            mailboxExecutor = context.getMailboxExecutor();
            return new JooqDatabaseBatchSinkWriter(properties, batchSize, batchIntervalMs);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Error initializing JooqDatabaseBatchSink", e);
        }
    }

    public static class JooqDatabaseBatchSinkWriter implements SinkWriter<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

        private final DSLContext dslContext;
        private final List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> batchBuffer;
        private final int batchSize;
        private final long batchIntervalMs;
        private final String topicName;
        public static EventPublisher eventPublisher;
        private long lastBatchTime;
        private transient ServiceLocator serviceLocator;
        private final Map<String, FileProgress> fileProgressMap;
        private final String baseUrl;
        private final ObjectMapper objectMapper;

        // Add this inner class to track progress
        private static class FileProgress {
            private int successCount;
            private int failCount;

            public void incrementSuccess() {
                successCount++;
            }

            public void incrementFail() {
                failCount++;
            }

            public int getSuccessCount() {
                return successCount;
            }

            public int getFailCount() {
                return failCount;
            }
        }

        public JooqDatabaseBatchSinkWriter(Properties properties, int batchSize, long batchIntervalMs) throws SQLException, ClassNotFoundException {
            System.setProperty("sun.net.maxDatagramSockets","4096");
            ExternalRegistryScanner.getInstance(properties);
            HikariDataSource hikariDataSource = DatabaseConnectionUtil.initConnectionPool(properties, 5);
            this.dslContext = DatabaseConnectionUtil.createPooledDSLContext(hikariDataSource);
            this.batchBuffer = new ArrayList<>();
            this.batchSize = batchSize;
            this.batchIntervalMs = batchIntervalMs;
            this.lastBatchTime = System.currentTimeMillis();
            CacheManager.getInstance(properties);
            this.serviceLocator = ServiceLocator.getInstance(dslContext);
            serviceLocator.registerSubClasses();
            this.fileProgressMap = new ConcurrentHashMap<>();
            this.baseUrl = properties.getProperty("api.base.url");
            this.objectMapper = new ObjectMapper();

            Properties kafkaProps = new Properties();
            kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventListenerDTOSerializer.class.getName());
            kafkaProps.put(ProducerConfig.ACKS_CONFIG, "1");
            this.topicName = DataStreamJob.getLobEventTopic(properties);
            this.eventPublisher = new EventPublisher(kafkaProps, topicName, mailboxExecutor);
        }

        public static EventPublisher getEventPublisher(){
            return eventPublisher;
        }

        @Override
        public void write(Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> value, Context context) throws IOException {
            LOG.info("Write method called with value: {}", value);
            try {
                String fileId = value.f0.getFileId();
                if (fileId != null && !fileId.isEmpty()) {
                    // Initialize progress tracker for this fileId if not exists
                    fileProgressMap.putIfAbsent(fileId, new FileProgress());
                }
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
                            for (CommonDataModel cdm : entry.getValue()) {
                                cdm.setReqId(data.getRequestId());
                                modelToRawDataMap.put(cdm, data);
                            }
                        }
                    }

                    for (Map.Entry<Class<? extends CommonDataModel>, Set<CommonDataModel>> entry : consolidatedModels.entrySet()) {
                        CommonDataModelService service = ServiceLocator.lookup(entry.getKey());
                        try {
                            service.batchSave(entry.getValue());
                            if(!entry.getKey().getSimpleName().equals(User.class.getSimpleName())) {
                                for (CommonDataModel model : entry.getValue()) {
                                    LOG.info("Operation performed is " + model.getOperationPerformed());
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    String fileId = rawData.getFileId();
                                    if (fileId != null && !fileId.isEmpty()) {
                                        fileProgressMap.get(fileId).incrementSuccess();
                                    }
                                    if (model.getOperationPerformed() != null && !model.getChanges().isEmpty()) {
                                        eventPublisher.publishEventAsync(
                                                rawData.getRequestId(),
                                                entry.getKey().getSimpleName(),
                                                rawData.getLob(),
                                                model.getChanges(),
                                                model.getOperationPerformed(),
                                                model.getId()
                                        );
                                    }
                                }
                            }
                            saveBatchIntegrationHistory(entry.getValue(), "SUCCESS", "Batch save successful");
                        } catch (Exception batchEx) {
                            LOG.error("Batch save failed. Falling back to individual saves.");
                            for (CommonDataModel model : entry.getValue()) {
                                try {
                                    service.batchSave(List.of(model));
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    String fileId = rawData.getFileId();
                                    // Track success
                                    if (fileId != null && !fileId.isEmpty()) {
                                        fileProgressMap.get(fileId).incrementSuccess();
                                    }
                                    if (model.getOperationPerformed() != null) {
                                        eventPublisher.publishEventAsync(
                                                rawData.getRequestId(),
                                                entry.getKey().getSimpleName(),
                                                rawData.getLob(),
                                                model.getChanges(),
                                                model.getOperationPerformed(),
                                                model.getId()
                                        );
                                    }
                                    saveIntegrationHistory(model, "SUCCESS", "Individual save successful");
                                } catch (Exception individualEx) {
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    String fileId = rawData.getFileId();
                                    if (fileId != null && !fileId.isEmpty()) {
                                        fileProgressMap.get(fileId).incrementFail();
                                    }
                                    saveIntegrationHistory(model, "FAILURE", "Save failed: " + individualEx.getMessage());
                                }
                            }
                        }
                    }

                    for (String fileId : fileProgressMap.keySet()) {
                        updateFileProgress(
                                fileId,
                                "OutletDetails",
                                getLobFromBatch(batchBuffer),
                                getJobIdFromBatch(batchBuffer)
                        );
                    }

                    fileProgressMap.clear();
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
                record.setEntityName(model.getClass().getSimpleName());
                records.add(record);
            }
            dslContext.batchInsert(records).execute();
        }

        @Override
        public void close() throws Exception {
            flush(true);
            eventPublisher.close();
//            if (connection != null) {
//                connection.close();
//            }
            LOG.info("Closed connection successfully");
        }

        private String getEntityTypeFromBatch(Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> consolidatedModels) {
            // Get the first entity type from the batch
            if (!consolidatedModels.isEmpty()) {
                return consolidatedModels.keySet().iterator().next().getSimpleName().toLowerCase();
            }
            return "unknown";
        }

        private String getLobFromBatch(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> buffer) {
            if (!buffer.isEmpty()) {
                return buffer.get(0).f0.getLob();
            }
            return "unknown";
        }

        private String getJobIdFromBatch(List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> buffer) {
            if (!buffer.isEmpty()) {
                return buffer.get(0).f0.getGroupId();
            }
            return "unknown";
        }

        private void updateFileProgress(String fileId, String entity, String lob, String jobId) {
            if (fileId == null || fileId.isEmpty()) {
                LOG.warn("Skipping update for empty fileId");
                return;
            }

            FileProgress progress = fileProgressMap.get(fileId);
            if (progress == null) {
                LOG.warn("No progress data found for fileId: {}", fileId);
                return;
            }

            try {
                // Construct the URL
                String url = String.format("%s/api/%s/master/%s/job/%s/unit/%s/update",
                        "http://localhost:8081", lob, entity, jobId, fileId);

                // Prepare the request body
                Map<String, Object> requestBody = new HashMap<>();
                Map<String, Object> progressMap = new HashMap<>();
                progressMap.put("consumerSuccessCount", progress.getSuccessCount());
                progressMap.put("consumerFailCount", progress.getFailCount());
                requestBody.put("progress", progressMap);

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                // Make the HTTP request
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    LOG.info("Successfully updated progress for fileId: {}, Success: {}, Fail: {}",
                            fileId, progress.getSuccessCount(), progress.getFailCount());
                } if (responseCode >= 200 && responseCode < 300) {
                    LOG.info("Successfully updated progress...");
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        StringBuilder response = new StringBuilder();
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        LOG.error("Failed to update progress for fileId: {}, response code: {}, message: {}",
                                fileId, responseCode, response.toString());
                    }
                }


                connection.disconnect();

            } catch (Exception e) {
                LOG.error("Error updating file progress for fileId: " + fileId, e);
            }
        }
    }
}