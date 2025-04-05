package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.etl.transformation.service.TransformerInfoRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class StreamingRawDataProcessor extends RichAsyncFunction<StreamingRawData, Tuple2<StreamingRawData, StreamingRawDataCdmCollector>> {
    private static final long serialVersionUID = -3351413046175753755L;

    private static final String TRANSFORMATION_ERROR = "Transformation Failed : ";
    private static final String SAVE_ERROR = "Error while saving record. Reason: ";

    private final Properties properties;

    private transient DSLContext dslContext;
    private transient EntityUtils entityUtils;
    private transient DataTransformationService dataTransformationService;
    private transient PreProcessPipelineService preProcessPipelineService;

    public StreamingRawDataProcessor(Properties commonProperties) {
        this.properties = Objects.requireNonNull(commonProperties, "Properties cannot be null");
    }


    @Override
    public void open(Configuration parameters) throws Exception {
        System.setProperty("sun.net.maxDatagramSockets", "2048");
        super.open(parameters);
        initializeResources();
    }

    private void initializeResources() throws Exception {
        SecurityContextUtils.initialize(properties);
        CacheManager.getInstance(properties);

        HikariDataSource hikariDataSource = DatabaseConnectionUtil.initConnectionPool(properties, 3);
        this.dslContext = DatabaseConnectionUtil.createPooledDSLContext(hikariDataSource);

        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.createInstance(properties);
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

        ServiceLocator serviceLocator = ServiceLocator.getInstance(dslContext);
        serviceLocator.registerSubClasses();
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (dslContext != null) {
            // dslContext.close();
        }
        System.out.println("Database connection closed.");
    }

    @Override
    public void asyncInvoke(StreamingRawData streamingRawData, ResultFuture resultFuture) {
        // Using Flink's directExecutor to execute tasks immediately
        org.apache.flink.util.concurrent.Executors.directExecutor().execute(() -> {
            try {
                StreamingRawDataCdmCollector collector = new StreamingRawDataCdmCollector();
                List<String> errorList = new ArrayList<>(); // Error tracking

                // Processing each transformer in the streaming data
                for (TransformerInfo transformerInfo : streamingRawData.getTransformerInfo()) {
                    processTransformer(streamingRawData, transformerInfo, collector, errorList);
                }

                if (!errorList.isEmpty()) {
                    streamingRawData.setStatus("Failure");
                    streamingRawData.setResponses(errorList.stream().map(errorMsg -> new StreamingRawData.Response("Failure", errorMsg)).collect(Collectors.toList()));
                    resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, Collections.emptyMap()))); // Handle failure case
                } else {
                    streamingRawData.setStatus("Processed");
                    resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, collector))); // Handle success case
                }
            } catch (Exception e) {
                log.error("Processing failed", e);
                streamingRawData.setStatus("Failure");
                resultFuture.complete(Collections.singletonList(Tuple2.of(streamingRawData, Collections.emptyMap())));
            }
        });
    }


    private void processTransformer(StreamingRawData streamingRawData, TransformerInfo transformerInfo, StreamingRawDataCdmCollector cdmCollector, List<String> errorList) {
        String transformerId = transformerInfo.getTransformerId();
        Class<? extends CommonDataModel> entityClass = entityUtils.getEntityClass(transformerInfo.getEntityName());

        try {
            long pstart = System.currentTimeMillis();
            List<CommonDataModel> transformedData = dataTransformationService.transformData(transformerId, entityClass, streamingRawData.getFeatures().get(0));
            long pstartTransform = System.currentTimeMillis();
            log.debug("Time to transform single record {}", pstartTransform - pstart);
            for (CommonDataModel cdm : transformedData) {
                PreProcessOperationResult preProcessOperationResult = preProcessPipelineService.preProcessPipeline(cdm, transformerInfo.getPreprocessValidationExcludeGroup());

                if (preProcessOperationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
                    preProcessPipelineService.evaluateFailures(preProcessOperationResult, errorList);
                } else {
                    cdmCollector.collect(entityClass, preProcessOperationResult.getPostValidationEnrichment().getOperationResultData());
                }
            }
        } catch (DataTransformationService.TransformationException e) {
            log.error("Transformation Exception ", e);
            errorList.add(TRANSFORMATION_ERROR + e.getMessage());
        } catch (Exception e) {
            log.error("Transformation Exception ", e);
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
