package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CommonDataModelService;
import com.applicate.services.channelkart.services.RegisterClassesService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.etl.transformation.service.TransformerInfoRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.flink.runtime.blob.BlobWriter.LOG;

public class StreamingRawDataProcessor extends ProcessFunction<StreamingRawData, CommonDataModel> {
    private static final long serialVersionUID = -3351413046175753755L;
    private static final String TRANSFORMATION_ERROR = "Transformation Failed : ";
    private static final String SAVE_ERROR = "Error while saving record. Reason: ";
    private final Properties properties;
    private transient Connection connection;
    private transient DSLContext dslContext;
    private transient EntityUtils entityUtils;
    private transient DataTransformationService dataTransformationService;
    private transient PreProcessPipelineService preProcessPipelineService;
    private transient RegisterClassesService registerClassesService;
    private static Logger LOG = LoggerFactory.getLogger(StreamingRawDataProcessor.class);
    public StreamingRawDataProcessor(Properties commonProperties) {
        this.properties = Objects.requireNonNull(commonProperties, "Properties cannot be null");
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        initializeResources();
    }

    private void initializeResources() throws Exception {
        // Create connection & DSLContext using the utility
        this.connection = DatabaseConnectionUtil.createConnection(properties);
        this.dslContext = DatabaseConnectionUtil.createDSLContext(connection);

        // Initialize services with dependency injection
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
        PreProcessPipelineService.getInstance(dataValidationService,dataEnrichmentService);
        LOG.info("registered classes");
        registerClassesService = new RegisterClassesService(dslContext);
        registerClassesService.registerSubClasses();
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
    public void processElement(StreamingRawData streamingRawData, Context ctx, Collector<CommonDataModel> out) throws Exception {
        try {
            List<TransformerInfo> transformerInfos = streamingRawData.getTransformerInfo();
            Map<Class<? extends CommonDataModel>, List<CommonDataModel>> dataset = new LinkedHashMap<>();
            List<String> errorList = new ArrayList<>();

            for (TransformerInfo transformerInfo : transformerInfos) {
                Class<? extends CommonDataModel> entityClass = entityUtils.getEntityClass(transformerInfo.getEntityName());
                processTransformer(streamingRawData, transformerInfo, dataset, errorList);
                if (!errorList.isEmpty()) {
                    streamingRawData.setStatus("Failure");
                    streamingRawData.setResponses(errorList.stream().map(s -> new StreamingRawData.Response("Failure", s)).collect(Collectors.toList()));
                    ctx.output(DataStreamJob.FAILED_TRANSFORMATIONS, streamingRawData);
                }
                else {
                    streamingRawData.setStatus("Success");
                    out.collect(dataset.get(entityClass).get(0));
                }
            }
//
//            if (errorList.isEmpty()) {
//                dispatchData(dataset, transformerInfos, errorList);
//            }

        } catch (Exception e) {
            streamingRawData.setStatus("Failure");
            ctx.output(DataStreamJob.FAILED_TRANSFORMATIONS, streamingRawData);
        }
    }

    private void processTransformer(StreamingRawData streamingRawData, TransformerInfo transformerInfo, Map<Class<? extends CommonDataModel>, List<CommonDataModel>> dataset, List<String> errorList) {
        String transformerId = transformerInfo.getTransformerId();
        Class<? extends CommonDataModel> entityClass = entityUtils.getEntityClass(transformerInfo.getEntityName());

       try {
           LOG.info("Transformation Called");
            List<CommonDataModel> transformedData = dataTransformationService.transformData(transformerId, entityClass, streamingRawData.getFeatures().get(0));
            for (CommonDataModel cdm : transformedData) {
                LOG.info("Pre Process Pipeline Called");

//                PreProcessOperationResult preProcessOperationResult = preProcessPipelineService.preProcessPipeline(cdm, transformerInfo.getPreprocessValidationExcludeGroup());
//
//                if (preProcessOperationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
//                    preProcessPipelineService.evaluateFailures(preProcessOperationResult, errorList);
//                } else {
                    dataset.computeIfAbsent(entityClass, k -> new ArrayList<>()).addAll(transformedData);
//                }
            }
        } catch (DataTransformationService.TransformationException e) {
            errorList.add(TRANSFORMATION_ERROR + e.getMessage());
        } catch (Exception e) {
            errorList.add("Unexpected error: " + e.getMessage());
        }
   }

    private void dispatchData(Map<Class<? extends CommonDataModel>, List<CommonDataModel>> dataset, List<TransformerInfo> transformerInfos, List<String> errorList) {
        try {
            LOG.info("Dispatch data called");
            Map<Class<?>, Set<CommonDataModel>> collector = new LinkedHashMap<>();

            for (Map.Entry<Class<? extends CommonDataModel>, List<CommonDataModel>> entry : dataset.entrySet()) {
                Class<? extends CommonDataModel> clazz = entry.getKey();
                CommonDataModelService cdmService = ServiceLocator.lookup(clazz);
                LOG.info("Class found is : " + cdmService);
                List<CommonDataModel> cdmList = entry.getValue();
                if (cdmList != null && !cdmList.isEmpty()) {
                    cdmService.save(cdmList.get(0)); // Pass the entire list for batch saving
                }
            }
        } catch (Throwable th) {
            errorList.add(SAVE_ERROR + ExceptionUtils.getRootCause(th).getMessage());
        }
    }

}
