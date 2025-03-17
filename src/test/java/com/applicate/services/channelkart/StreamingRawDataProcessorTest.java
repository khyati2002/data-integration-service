package com.applicate.services.channelkart;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.HierarchyMetadataRepository;
import com.applicate.services.channelkart.repository.UserParentRepository;
import com.applicate.services.channelkart.services.*;
import com.applicate.services.channelkart.transformers.impl.JoltTransformer;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.*;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.etl.transformation.service.TransformerInfoRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.utils.ReflectionUtils;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;

public class StreamingRawDataProcessorTest {
    private Connection connection;
    private DSLContext dsl;
    private ExternalRegistryScanner externalRegistryScanner;
    private ETLRegistry etlRegistry;
    private TransformerInfoRegistry transformerInfoRegistry;
    private EntityUtils entityUtils;
    private DataTransformationService dataTransformationService;
    private Collector<StreamingRawData> collector;
    private StreamingRawDataProcessor processor;
    private Properties properties;

    private void resetEntityUtilsSingleton() throws Exception {
        Field instanceField = EntityUtils.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @BeforeEach
    void setUp() throws Exception {
         Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(null);
//        Map<String, Properties> applicationProperties = new HashMap<>();
        Properties properties = applicationProperties.getOrDefault("Common", new Properties());
        connection = DatabaseConnectionUtil.createConnection(properties);
        this.dsl = DatabaseConnectionUtil.createDSLContext(connection);

//
//        // Reset any previous singleton instance to use the current DSLContext.
//        resetEntityUtilsSingleton();
//
//        entityUtils = EntityUtils.getInstance(dsl);
//        externalRegistryScanner = ExternalRegistryScanner.getInstance(properties);
//        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
//        transformerInfoRegistry = TransformerInfoRegistry.getInstance(dsl);
//        dataTransformationService = DataTransformationService.getInstance(transformerInfoRegistry,etlRegistry,entityUtils);
////        userService = UserService.getInstance(dsl);
////        outletDetailsService = OutletDetailsService.getInstance(dsl);
//        processor = StreamingRawDataProcessor.getInstance(properties);
//        Set<Class<? extends AbstractCDMService>> subClasses = ReflectionUtils.findSubClasses(AbstractCDMService.class);
//
//        for (Class<? extends AbstractCDMService> serviceClass : subClasses) {
//            try {
//                // Create an instance using the default constructor
//                AbstractCDMService serviceInstance = serviceClass.getDeclaredConstructor(DSLContext.class).newInstance(dsl);
//
//                // Get the entity class it handles (assuming each service has a getPersistentClass() method)
//                Class<?> persistentClass = serviceInstance.getClass();
//
//                // Register the service
//                ServiceLocator.register(persistentClass, serviceInstance);
//            } catch (Exception e) {
//                System.err.println("Failed to register service: " + serviceClass.getName());
//                e.printStackTrace();
//            }
//        }
//
//        TransformerInfo ckTransformerInfo = new TransformerInfo();
//        ckTransformerInfo.setType("OutletDetails");
//        ckTransformerInfo.setImplementation("com.applicate.services.channelkart.transformers.impl.JoltTransformer");
//        ckTransformerInfo.setActiveStatus(ActiveStatus.ACTIVE);
    }
    @Test
    void testProcessElement() throws Exception {
        // Mock input data

        String message = "{\"requestId\":\"aca18457-e436-46ee-b1f5-e03f2fa68c0a\",\"groupId\":\"2024-12-19\",\"fileId\":null,\"lob\":\"ckuatunnati\",\"submittedBy\":null,\"transformerInfo\":[{\"entityName\":\"OutletDetails\",\"transformerId\":\"unnati_csp_outlet_master_mdm\",\"operationType\":\"insert\",\"preprocessValidationExcludeGroup\":\"outlet_validation_exclude\"}],\"topicName\":\"flink-test\",\"preserveOnFailure\":true,\"features\":[{\"UID\":\"180600162461\",\"CREATIONDATE\":\"2024-12-19 02:48:11.067\",\"PICKUPDATE\":null,\"DISTRICT\":\"NDIS\",\"Branch\":\"NDEL\",\"CUSTName\":\"DHANVANTRI PHARMACY & COSMETICS\",\"OwnerName\":\"DHANVANTRI PHARMACY & COSMETICS\",\"ChannelType\":\"Retail\",\"OutletType\":\"Chemist\",\"LoyaltyType\":\"FC Enrolled PCP\",\"FoodsTier\":null,\"PCPTier\":null,\"CustAddress\":\"SHOP NO.-G-7 & G8  MALIK BUILDCON PLAZA-1  PLOT NO\",\"CustState\":null,\"CustCity\":null,\"PIN\":null,\"Mobile\":null,\"BirthDate\":null,\"Anniversary\":null,\"PCPSubType\":null,\"FCFoodsSubType\":null,\"ITCProducts\":\"Y\",\"GiftVoucher\":\"Y\",\"OutletLat\":null,\"OutletLong\":null,\"CustOrder\":\"Y\",\"CustLoyalty\":\"N\",\"AutoRedemption\":\"Y\",\"Active\":\"Y\",\"TYPE\":\"non loyalty\",\"OutletName\":\"DHANVANTRI PHARMACY & COSMETICS\",\"supplierMapping\":[{\"CustID\":\"1088\",\"SIFYID\":\"DE5141CIS4961088\",\"WDDest\":\"DE5141\",\"UID\":\"180600162461\",\"RCSID\":\"180600162461\",\"WDName\":\"FARIDABAD MARKETING\"}]}],\"loginId\":\"integration_user\",\"offset\":null,\"retryCount\":null,\"ignoreS3Log\":false,\"headersMap\":null},\n";
        StreamingRawData streamingRawData1 = JSONUtils.getObjectMapper().readValue(message,StreamingRawData.class);
        processor.processElement(streamingRawData1, null, collector);


    }

}
