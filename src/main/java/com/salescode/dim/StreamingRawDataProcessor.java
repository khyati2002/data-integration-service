package com.salescode.dim;

import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.client.properties.PropertyService;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.cache.RedisIdleEvictionManager;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.etl.transformation.service.TransformerInfoRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.records.CkIntegrationHistoryRecord;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.utils.InsightsUtils;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.enums.ProgressStage;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static com.salescode.dim.jooq.generated.tables.CkIntegrationHistory.CK_INTEGRATION_HISTORY;

public class StreamingRawDataProcessor extends RichAsyncFunction<StreamingRawData, Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {
    private static final long serialVersionUID = -3351413046175753755L;
    private static final String TRANSFORMATION_ERROR = "Transformation Failed : ";
    private static final String SAVE_ERROR = "Error while saving record. Reason: ";
    private final Properties properties;
    private transient Connection connection;
    private transient DSLContext dslContext;
    private transient EntityUtils entityUtils;
    private transient DataTransformationService dataTransformationService;
    private transient PreProcessPipelineService preProcessPipelineService;
    private transient String topicName;
    private transient boolean insightsEnabled;
    private KafkaProducer<String, FileProgressEvent> producer;
    Logger logger = LoggerFactory.getLogger(StreamingRawDataProcessor.class);

    public StreamingRawDataProcessor(Properties commonProperties) {
        this.properties = Objects.requireNonNull(commonProperties, "Properties cannot be null");
    }


    @Override
    public void open(Configuration parameters) throws Exception {

        System.setProperty("sun.net.maxDatagramSockets","4096");
        super.open(parameters);
        Properties kafkaInsightsProps = new Properties();
        kafkaInsightsProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("bootstrap.servers"));
        kafkaInsightsProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        kafkaInsightsProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, FileProgressEventSerializer.class.getName());
        kafkaInsightsProps.put(ProducerConfig.ACKS_CONFIG, "1");

        producer = new KafkaProducer<>(kafkaInsightsProps);
        initializeResources();
    }

    private void initializeResources() throws Exception {
        // Create connection & DSLContext using the utility
//        this.connection = DatabaseConnectionUtil.createConnection(properties);
//        this.dslContext = DatabaseConnectionUtil.createDSLContext(connection);
        HikariDataSource hikariDataSource = DatabaseConnectionUtil.initConnectionPool(properties, 4);
        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext(hikariDataSource);

        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance(properties);
        ETLRegistry etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);

        // Entity utils initialization
        entityUtils = EntityUtils.getInstance(dslContext);

        // Setup transformation service
        TransformerInfoRegistry transformerInfoRegistry = new TransformerInfoRegistry(dslContext);
        dataTransformationService = new DataTransformationService(transformerInfoRegistry, etlRegistry, entityUtils);

        // Setup enrichment service
        EnrichmentInfoRegistry enrichmentInfoRegistry = new EnrichmentInfoRegistry(dslContext);
        DataEnrichmentService dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);

        // Setup validation service
        ValidationInfoRegistry validationRegistry = new ValidationInfoRegistry(dslContext);
        ValidationExcludeGroupRegistry validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(dslContext);
        DataValidationService dataValidationService = new DataValidationService(validationRegistry, validationExcludeGroupRegistry, etlRegistry);

        // Initialize pipeline service
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
        DistributedCache.getInstance(properties);
        SecurityContextUtils.getInstance(properties);
        ServiceLocator serviceLocator = ServiceLocator.getInstance(dslContext);
        serviceLocator.registerSubClasses();

        topicName = properties.getProperty("insights.topic");
        insightsEnabled = Boolean.parseBoolean(properties.getProperty("insights.enabled"));
        PropertyService propertyService = new PropertyService((MetaDataService) ServiceLocator.lookup(Metadata.class));
        PropertyRegistry.getInstance(propertyService);

    }

    @Override
    public void close() throws Exception {
        super.close();
        if (dslContext != null) {
            // dslContext.close();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        System.out.println("Database connection closed.");
    }

    @Override
    public void asyncInvoke(StreamingRawData streamingRawData, ResultFuture<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> resultFuture) {
        // Using Flink's directExecutor to execute tasks immediately
        org.apache.flink.util.concurrent.Executors.directExecutor().execute(() -> {
            try {
                long start = System.currentTimeMillis();
                Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataset = new LinkedHashMap<>(); // Data storage
                List<String> errorList = new ArrayList<>(); // Error tracking
                if(insightsEnabled) {
                    sendToKafkaPublisherUpdate(streamingRawData, ProgressStage.QUEUE);
                    sendToKafkaPublisherUpdate(streamingRawData, ProgressStage.PROCESS);
                }
                // Processing each transformer in the streaming data
                for (TransformerInfo transformerInfo : streamingRawData.getTransformerInfo()) {
                    processTransformer(streamingRawData, transformerInfo, dataset, errorList);
                }

                if (!errorList.isEmpty()) {
                    streamingRawData.setStatus("Failure");
                    saveIntegrationHistory(streamingRawData, "FAILURE", "Save failed: " + errorList);
                    streamingRawData.setResponses(
                            errorList.stream()
                                    .map(errorMsg -> new StreamingRawData.Response("Failure", errorMsg))
                                    .collect(Collectors.toList())
                    );
                    if(insightsEnabled) {
                        sendToKafkaConsumerUpdate(streamingRawData, 0, 1, 0);
                    }
                    resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, Collections.emptyMap()))); // Handle failure case
                } else {
                    streamingRawData.setStatus("Success");
                    if(entityUtils.getEntityClass(streamingRawData.getTransformerInfo().get(0).getEntityName()).getSimpleName().equals(OutletDetails.class.getSimpleName())) {
                        PreProcessOperationResult res = preProcessPipelineService.preProcessPipeline(dataset.values().stream()
                                .flatMap(Set::stream)
                                .findFirst()
                                .orElse(null), "");
                        if (res.getStatus().equals(PreProcessOperationResult.Status.FAILURE)) {
                            streamingRawData.setStatus("Failure");
                            saveIntegrationHistory(streamingRawData, "FAILURE", "Save failed: " + errorList);
                            streamingRawData.setResponses(
                                    errorList.stream()
                                            .map(errorMsg -> new StreamingRawData.Response("Failure", errorMsg))
                                            .collect(Collectors.toList())
                            );
                            if(insightsEnabled) {
                                sendToKafkaConsumerUpdate(streamingRawData, 0, 1, 0);
                            }
                            resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, Collections.emptyMap()))); // Handle failure case
                        }
                    }

                    if(streamingRawData.getStatus().equals("Success")){
                        resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, dataset)));
                    }
                    // Handle success case
                }
            } catch (Exception e) {
                logger.error("Processing failed", e);
                streamingRawData.setStatus("Failure");
                resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, Collections.emptyMap())));
            }
        });
    }
    private void sendToKafkaPublisherUpdate(StreamingRawData streamingRawData, ProgressStage stage) {
        try {
            String topic = topicName;
            long idleEvictionTTL = Long.parseLong(properties.getProperty("INSIGHTS_INTEGRATION_IDLE_EVICTION_TTL_MINUTES"));
            if(streamingRawData.getFileId()==null){
                streamingRawData.setFileId(RedisIdleEvictionManager.getInstance().getOrCreateFileId(streamingRawData.getLob(),streamingRawData.getTransformerInfo().get(0).getEntityName(), "fileId", idleEvictionTTL, TimeUnit.MINUTES));
            }
            FileProgressEvent message = InsightsUtils.createRequest(streamingRawData,1,0,0,stage);

            ProducerRecord<String, FileProgressEvent> record = new ProducerRecord<>(topic, streamingRawData.getFileId(), message);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending data to Kafka insights", exception);
                } else {
                    logger.info("Successfully sent data to Kafka insights. Offset: " + metadata.offset());
                }
            });
        } catch (Exception e) {
            logger.error("Error while sending to Kafka", e);
        }
    }
    private void sendToKafkaConsumerUpdate(StreamingRawData streamingRawData,long successCount,long logicalFailureCount,long serverFailureCount) {
        try {
            String topic = topicName;
            long idleEvictionTTL = Long.parseLong(properties.getProperty("INSIGHTS_INTEGRATION_IDLE_EVICTION_TTL_MINUTES"));
            if(streamingRawData.getFileId()==null){
                streamingRawData.setFileId(RedisIdleEvictionManager.getInstance().getOrCreateFileId(streamingRawData.getLob(),streamingRawData.getTransformerInfo().get(0)
                        .getEntityName(), "fileId", idleEvictionTTL, TimeUnit.MINUTES));
            }
            FileProgressEvent message= InsightsUtils.createRequest(streamingRawData,successCount,logicalFailureCount,serverFailureCount,ProgressStage.SAVE);
            ProducerRecord<String, FileProgressEvent> record = new ProducerRecord<>(topic, streamingRawData.getFileId(), message);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending data to Kafka insights", exception);
                } else {
                    logger.info("Successfully sent data to Kafka insights. Offset: " + metadata.offset());
                }
            });
        } catch (Exception e) {
            logger.error("Error while sending to Kafka", e);
        }
    }
    private void saveIntegrationHistory(StreamingRawData model, String status, String message) {
        CkIntegrationHistoryRecord record = new CkIntegrationHistoryRecord();
        record.setId(UUID.randomUUID().toString());
        record.setEntityName(model.getTransformerInfo().get(0).getEntityName());
        record.setRequestId(model.getRequestId());
        record.setStatus(status);
        record.setDescription(message);
        record.setTimestamp(Instant.now().toEpochMilli());
        dslContext.insertInto(CK_INTEGRATION_HISTORY).set(record).execute();
    }


    private void processTransformer(StreamingRawData streamingRawData, TransformerInfo transformerInfo, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataset, List<String> errorList) {
        String transformerId = transformerInfo.getTransformerId();
        Class<? extends CommonDataModel> entityClass = entityUtils.getEntityClass(transformerInfo.getEntityName());

        try {
            long pstart = System.currentTimeMillis();
            List<CommonDataModel> transformedData = dataTransformationService.transformData(transformerId, entityClass, streamingRawData.getFeatures()
                    .get(0));
            long pstartTransform = System.currentTimeMillis();
            logger.info("Time to transform single record {}", pstartTransform - pstart);
            for (CommonDataModel cdm : transformedData) {
                PreProcessOperationResult preProcessOperationResult = preProcessPipelineService.preProcessPipeline(cdm, transformerInfo.getPreprocessValidationExcludeGroup());

                if (preProcessOperationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
                    preProcessPipelineService.evaluateFailures(preProcessOperationResult, errorList);
                } else {
                    dataset.computeIfAbsent(entityClass, k -> new HashSet<>())
                            .addAll(preProcessOperationResult.getPostValidationEnrichment().getOperationResultData());
                }
            }
        } catch (DataTransformationService.TransformationException e) {
            logger.error("Transformation Exception ", e);
            logger.error("Transformation Exception ", e.getStackTrace());
            errorList.add(TRANSFORMATION_ERROR + e.getMessage());
        } catch (Exception e) {
            logger.error("Transformation Exception ", e);
            logger.error("Transformation Exception ", e.getStackTrace());
            errorList.add("Unexpected error: " + e.getMessage());
        }
    }

    private void dispatchData(Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataset, List<TransformerInfo> transformerInfos, List<String> errorList) {
        try {
            // MDMDispatcher.dispatch(dataset, transformerInfos); // Uncomment when ready
        } catch (Throwable th) {
            errorList.add(SAVE_ERROR + ExceptionUtils.getRootCause(th).getMessage());
        }
    }

}
