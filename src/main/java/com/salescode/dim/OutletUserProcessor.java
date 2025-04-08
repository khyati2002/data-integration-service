package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.jooq.DSLContext;

import java.util.*;

public class OutletUserProcessor extends RichAsyncFunction<
        Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>,
        Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

    private transient PreProcessPipelineService preProcessPipelineService;
    private transient EntityUtils entityUtils;

    private final Properties properties;

    public OutletUserProcessor(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        HikariDataSource hikariDataSource = DatabaseConnectionUtil.initConnectionPool(properties, 4);
        DSLContext dslContext = DatabaseConnectionUtil.createPooledDSLContext(hikariDataSource);

        // Set up services like in your main processor
        ValidationInfoRegistry validationRegistry = new ValidationInfoRegistry(dslContext);
        ValidationExcludeGroupRegistry validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(dslContext);
        ETLRegistry etlRegistry = ETLRegistry.getInstance(ExternalRegistryScanner.getInstance(properties));
        DataValidationService validationService = new DataValidationService(validationRegistry, validationExcludeGroupRegistry, etlRegistry);

        EnrichmentInfoRegistry enrichmentRegistry = new EnrichmentInfoRegistry(dslContext);
        DataEnrichmentService enrichmentService = new DataEnrichmentService(enrichmentRegistry, etlRegistry);

        preProcessPipelineService = new PreProcessPipelineService(validationService, enrichmentService);
        entityUtils = EntityUtils.getInstance(dslContext);
    }

    @Override
    public void asyncInvoke(
            Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> input,
            ResultFuture<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> resultFuture
    ) {
        org.apache.flink.util.concurrent.Executors.directExecutor().execute(() -> {
            try {
                StreamingRawData rawData = input.f0;
                Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataset = input.f1;

                if (!dataset.containsKey(OutletDetails.class)) {
                    resultFuture.complete(Collections.singletonList(input));
                    return;
                }

                Set<CommonDataModel> outlets = dataset.get(OutletDetails.class);
                Set<CommonDataModel> updatedOutlets = new HashSet<>();

                for (CommonDataModel cdm : outlets) {
                    if (cdm instanceof OutletDetails ) {
                        OutletDetails outlet = (OutletDetails) cdm;
                        if (outlet.getUserName() != null) {
                            outlet.getUserName().setLocationHierarchy(outlet.getLocation());

                            PreProcessOperationResult result = preProcessPipelineService
                                    .preProcessPipeline(outlet.getUserName(), "");

                            if (result.getStatus() == PreProcessOperationResult.Status.FAILURE) {
                                rawData.setStatus("Failure");
                                resultFuture.complete(Collections.singletonList(Tuple2.of(rawData, Collections.emptyMap())));
                            }
                        }

                        updatedOutlets.add(outlet);
                    }
                }

                dataset.put(OutletDetails.class, updatedOutlets);
                resultFuture.complete(Collections.singletonList(Tuple2.of(rawData, dataset)));

            } catch (Exception e) {
                // On failure, return original input
                resultFuture.complete(Collections.singletonList(input));
            }
        });
    }
}
