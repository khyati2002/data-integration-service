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
import com.salescode.dim.kafka.FailurePublisher;
import com.salescode.dim.kafka.InsightsPublisher;
import com.salescode.dim.kafka.SuccessPublisher;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

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
        this.batchSize = Integer.parseInt(dbProperties.getProperty("batch.size", "100"));
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
        private final String failureTopicName;
        private final String insightsTopicName;
        private final String successTopicName;
        public static EventPublisher eventPublisher;
        public static InsightsPublisher insightsPublisher;
        public static FailurePublisher failurePublisher;
        public static SuccessPublisher successPublisher;
        private long lastBatchTime;
        private transient ServiceLocator serviceLocator;
        private final String baseUrl;
        private final ObjectMapper objectMapper;
        private boolean insightsEnabled;
        private boolean publishSuccessEnabled;

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
            this.baseUrl = properties.getProperty("api.base.url");
            this.objectMapper = new ObjectMapper();
            this.insightsEnabled = Boolean.parseBoolean(properties.getProperty("insights.enabled"));
            this.publishSuccessEnabled = Boolean.parseBoolean(properties.getProperty("publish.success.enabled"));
            Properties kafkaProps = new Properties();
            kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventListenerDTOSerializer.class.getName());
            kafkaProps.put(ProducerConfig.ACKS_CONFIG, "1");

            Properties kafkaFailureTopicProps = new Properties();
            kafkaFailureTopicProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaFailureTopicProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaFailureTopicProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StreamingRawDataSerializer.class.getName());
            kafkaFailureTopicProps.put(ProducerConfig.ACKS_CONFIG, "1");


            Properties kafkaInsightsProps = new Properties();
            kafkaInsightsProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaInsightsProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaInsightsProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, FileProgressEventSerializer.class.getName());
            kafkaInsightsProps.put(ProducerConfig.ACKS_CONFIG, "1");

            Properties kafkaSuccessTopicProps = new Properties();
            kafkaSuccessTopicProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
            kafkaSuccessTopicProps .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            kafkaSuccessTopicProps .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StreamingRawDataSerializerSuccess.class.getName());
            kafkaSuccessTopicProps .put(ProducerConfig.ACKS_CONFIG, "1");

            this.topicName = DataStreamJob.getLobEventTopic(properties);
            this.failureTopicName = DataStreamJob.getLobFailureTopic(properties);
            this.insightsTopicName = properties.getProperty("insights.topic");
            this.successTopicName = DataStreamJob.getLobSuccessTopic(properties);
            this.eventPublisher = new EventPublisher(kafkaProps, topicName, mailboxExecutor);
            this.insightsPublisher = new InsightsPublisher(kafkaInsightsProps, insightsTopicName, mailboxExecutor);
            this.failurePublisher = new FailurePublisher(kafkaFailureTopicProps, failureTopicName, mailboxExecutor);
            this.successPublisher = new SuccessPublisher(kafkaSuccessTopicProps, successTopicName, mailboxExecutor);
        }

        public static EventPublisher getEventPublisher(){
            return eventPublisher;
        }

        @Override
        public void write(Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> value, Context context) throws IOException {
            LOG.info("Write method called with value: {}", value);
            try {
                String fileId = value.f0.getFileId();
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

        public static String getStackTraceAsString(Throwable throwable) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            return sw.toString();
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

                            for (CommonDataModel model : entry.getValue()) {
                                LOG.info("Operation performed is " + model.getOperationPerformed());
                                StreamingRawData rawData = modelToRawDataMap.get(model);
                                String fileId = rawData.getFileId();
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

                            if (insightsEnabled && !entry.getValue().isEmpty()) {
                                // Group models by fileId and master (composite key)
                                Map<String, List<CommonDataModel>> groupedModels = entry.getValue()
                                        .stream()
                                        .collect(Collectors.groupingBy(model -> {
                                            StreamingRawData rawData = modelToRawDataMap.get(model);
                                            // Create composite key: fileId + master
                                            return rawData.getFileId() + "_" + rawData.getTransformerInfo().get(0).getEntityName(); // Adjust based on how you access master
                                        }));

                                // Publish insights for each group
                                for (Map.Entry<String, List<CommonDataModel>> groupEntry : groupedModels.entrySet()) {
                                    List<CommonDataModel> groupedList = groupEntry.getValue();
                                    if (!groupedList.isEmpty()) {
                                        CommonDataModel firstModel = groupedList.get(0);
                                        StreamingRawData rawData = modelToRawDataMap.get(firstModel);
                                        insightsPublisher.publishEventAsync(
                                                rawData,
                                                groupedList.size(),
                                                0,
                                                0
                                        );
                                    }
                                }
                            }


                            if(publishSuccessEnabled){
                                for (CommonDataModel model : entry.getValue()) {
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    successPublisher.publishEventAsync(rawData);
                                }
                            }
                        } catch (Exception batchEx) {
                            LOG.error("Batch save failed. Falling back to individual saves.", batchEx);
                            for (CommonDataModel model : entry.getValue()) {
                                try {
                                    service.batchSave(List.of(model));
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    String fileId = rawData.getFileId();

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
 //                                   saveIntegrationHistory(model, "SUCCESS", "Individual save successful");
                                    if(insightsEnabled) {
                                        insightsPublisher.publishEventAsync(
                                                rawData,
                                                1,
                                                0,
                                                0
                                        );
                                    }
                                    if(publishSuccessEnabled){
                                        successPublisher.publishEventAsync(rawData);
                                    }
                                } catch (Exception individualEx) {
                                    LOG.error("Individual Exception for {}", model.getId(), individualEx);
                                    StreamingRawData rawData = modelToRawDataMap.get(model);
                                    String fileId = rawData.getFileId();
                                    String fullStackTrace = getStackTraceAsString(individualEx); // See utility method below
                                    String truncatedStackTrace = fullStackTrace.length() > 1000
                                            ? fullStackTrace.substring(0, 1000)
                                            : fullStackTrace;
                                    saveIntegrationHistory(model, "FAILURE", "Save failed: " + (individualEx.getMessage() != null ? individualEx.getMessage() : "") + truncatedStackTrace);
                                    if(rawData.getResponses() == null){
                                        rawData.setResponses(new ArrayList<>());
                                    }
                                    rawData.getResponses().add(new StreamingRawData.Response(
                                            "FAILURE",
                                            "Save failed: " + individualEx.getMessage()));

                                    failurePublisher.publishEventAsync(
                                            rawData
                                    );
                                    if(insightsEnabled) {
                                        insightsPublisher.publishEventAsync(
                                                rawData,
                                                0,
                                                1,
                                                0
                                        );
                                    }
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
            record.setEntityName(model.getClass().getSimpleName());
            record.setRequestId(model.getReqId());
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
    }
}