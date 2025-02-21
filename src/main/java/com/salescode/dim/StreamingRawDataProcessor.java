package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.registry.ETLRegistry;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.transformers.registry.TransformerInfoRegistry;
import com.salescode.dim.transformers.service.DataTransformationService;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StreamingRawDataProcessor extends ProcessFunction<StreamingRawData, StreamingRawData> {

    private static final long serialVersionUID = -3351413046175753755L;
    private final Properties properties;
    private transient Connection connection;
    private transient DSLContext dslContext;
    private transient ExternalRegistryScanner externalRegistryScanner;
    private transient ETLRegistry etlRegistry;
    private transient TransformerInfoRegistry transformerInfoRegistry;
    private transient EntityUtils entityUtils;
    private transient DataTransformationService dataTransformationService;

    public StreamingRawDataProcessor(Properties commonProperties) {
        this.properties = commonProperties;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        // Create connection & DSLContext using the utility
        this.connection = DatabaseConnectionUtil.createConnection(properties);
        this.dslContext = DatabaseConnectionUtil.createDSLContext(connection);
        externalRegistryScanner = ExternalRegistryScanner.getInstance(properties);
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        entityUtils = EntityUtils.getInstance(dslContext);
        transformerInfoRegistry = TransformerInfoRegistry.getInstance(dslContext);
        dataTransformationService = DataTransformationService.getInstance(transformerInfoRegistry, etlRegistry, entityUtils);
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
    public void processElement(StreamingRawData streamingRawData, Context ctx, Collector<StreamingRawData> out) throws Exception {
        List<CommonDataModel> transformedObjects = new ArrayList<>();
        List<TransformerInfo> transformerInfos = streamingRawData.getTransformerInfo();
        for (TransformerInfo transformerInfo : transformerInfos) {
            String transformerId = transformerInfo.getTransformerId();
            String entityName = transformerInfo.getEntityName();
            JsonNode jsonNode = streamingRawData.getFeatures().get(0);
            List<? extends CommonDataModel> cdms = dataTransformationService.transformData(transformerId, entityName, jsonNode);
            System.out.printf(JSONUtils.getObjectMapper().convertValue(cdms, com.fasterxml.jackson.databind.JsonNode.class).toPrettyString());

        }
    }

}