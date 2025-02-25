package com.salescode.dim.transformers.service;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.salescode.dim.DatabaseConnectionUtil;
import com.salescode.dim.PropertyLoader;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import com.salescode.dim.registry.ETLRegistry;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.transformers.AbstractTransformer;
import com.salescode.dim.transformers.registry.TransformerInfoRegistry;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class DataTransformationServiceTest {

    private static final String DUMMY_TRANSFORMER_ID = "dummyTransformerId";
    private static final String NULL_TRANSFORMER_ID = "nullTransformerId";
    private static final String ENTITY_NAME = "TestEntity";

    private Connection connection;
    private DSLContext dsl;
    private DataTransformationService dataTransformationService;
    private TransformerInfoRegistry transformerInfoRegistry;
    private DummyETLRegistry dummyETLRegistry;
    private DummyEntityUtils dummyEntityUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        // Load application properties and create a connection using DatabaseConnectionUtil.
        Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(null);
        Properties properties = applicationProperties.getOrDefault("Common", new Properties());
        connection = DatabaseConnectionUtil.createConnection(properties);
        this.dsl = DatabaseConnectionUtil.createDSLContext(connection);

        // Reset any previous singleton instances.
        resetSingleton(TransformerInfoRegistry.class, "instance");
        resetSingleton(DataTransformationService.class, "instance");

        // Create the TransformerInfoRegistry instance using the DSLContext.
        transformerInfoRegistry = TransformerInfoRegistry.getInstance(dsl);

        // Pre-populate the registry's cache with dummy transformer info records.
        populateTransformerCache(transformerInfoRegistry, DUMMY_TRANSFORMER_ID, createDummyInfo(DUMMY_TRANSFORMER_ID, "dummyTransformer", "DummyTransformer"));
        populateTransformerCache(transformerInfoRegistry, NULL_TRANSFORMER_ID, createDummyInfo(NULL_TRANSFORMER_ID, "nullTransformer", "NullTransformer"));

        // Create dummy ETLRegistry and EntityUtils implementations.
        dummyETLRegistry = new DummyETLRegistry(ExternalRegistryScanner.getInstance(properties));
        dummyEntityUtils = new DummyEntityUtils(dsl);
        objectMapper = new ObjectMapper();

        // Create the DataTransformationService singleton.
        dataTransformationService = DataTransformationService.getInstance(transformerInfoRegistry, dummyETLRegistry, dummyEntityUtils);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Resets the singleton instance for the given class and field name.
     */
    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        Field instanceField = clazz.getDeclaredField(fieldName);
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    /**
     * Creates a dummy TransformerInfo with the specified values.
     */
    private TransformerInfo createDummyInfo(String id, String implementation, String name) {
        TransformerInfo info = new TransformerInfo();
        info.setId(id);
        info.setImplementation(implementation);
        info.setName(name);
        return info;
    }

    /**
     * Populates the TransformerInfoRegistry's internal caches via reflection.
     */
    private void populateTransformerCache(TransformerInfoRegistry registry, String id, TransformerInfo info) throws Exception {
        Field cacheField = TransformerInfoRegistry.class.getDeclaredField("transformerCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, TransformerInfo> cache = (Map<String, TransformerInfo>) cacheField.get(registry);
        cache.put(id, info);

        Field nameCacheField = TransformerInfoRegistry.class.getDeclaredField("nameToIdCache");
        nameCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> nameCache = (Map<String, String>) nameCacheField.get(registry);
        nameCache.put(info.getName(), info.getId());
    }

    /**
     * Test transformData when no transformerId is provided.
     * The input map is converted directly to a CommonDataModel.
     */
    @Test
    public void testTransformDataWithoutTransformer() {
        Map<String, Object> input = new HashMap<>();
        input.put("value", "originalValue");

        // When transformerId is null, no transformation is applied.
        List<? extends CommonDataModel> result = dataTransformationService.transformData(null, ENTITY_NAME, input);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        TestCommonDataModel model = (TestCommonDataModel) result.get(0);
        assertEquals("originalValue", model.getValue());
    }

    /**
     * Test transformData when a valid transformerId is provided.
     * The dummy transformer returns a map with "value" set to "transformed".
     */
    @Test
    public void testTransformDataWithTransformer() {
        Map<String, Object> input = new HashMap<>();
        input.put("value", "ignoredInput");

        List<? extends CommonDataModel> result = dataTransformationService.transformData(DUMMY_TRANSFORMER_ID, ENTITY_NAME, input);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        TestCommonDataModel model = (TestCommonDataModel) result.get(0);
        // DummyTransformer returns a map with "value" = "transformed"
        assertEquals("transformed", model.getValue());
    }

    /**
     * Test transformData using a JsonNode input.
     */
    @Test
    public void testTransformDataWithJsonNode() throws Exception {
        String json = "{\"value\": \"jsonValue\"}";
        JsonNode jsonNode = objectMapper.readTree(json);

        List<? extends CommonDataModel> result = dataTransformationService.transformData(null, ENTITY_NAME, jsonNode);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        TestCommonDataModel model = (TestCommonDataModel) result.get(0);
        assertEquals("jsonValue", model.getValue());
    }

    /**
     * Test that transformData throws a RuntimeException when the transformer returns null.
     */
    @Test
    public void testTransformDataWithNullTransformedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("value", "anything");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dataTransformationService.transformData(NULL_TRANSFORMER_ID, ENTITY_NAME, input)
        );
        assertTrue(exception.getMessage().contains("transformedData cannot be null"));
    }

    // --- Dummy Implementations ---

    /**
     * Dummy ETLRegistry implementation.
     */
    private static class DummyETLRegistry extends ETLRegistry {
        protected DummyETLRegistry(ExternalRegistryScanner externalRegistryScanner) {
            super(externalRegistryScanner);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getTransformer(String implementation) {
            if ("dummyTransformer".equals(implementation)) {
                return (T) new DummyTransformer();
            } else if ("nullTransformer".equals(implementation)) {
                return (T) new DummyNullTransformer();
            }
            throw new IllegalArgumentException("Unknown transformer implementation: " + implementation);
        }
    }

    /**
     * Dummy transformer that returns a map with "value" set to "transformed".
     */
    private static class DummyTransformer extends AbstractTransformer<Map<String, Object>, Object> {
        private TransformerInfo transformerInfo;
        @Override
        public Object transform(Map<String, Object> input) {
            return Map.of("value", "transformed");
        }
        @Override
        public void setTransformerInfo(TransformerInfo transformerInfo) {
            this.transformerInfo = transformerInfo;
        }
    }

    /**
     * Dummy transformer that returns null.
     */
    private static class DummyNullTransformer extends AbstractTransformer<Map<String, Object>, Object> {
        @Override
        public Object transform(Map<String, Object> input) {
            return null;
        }
        @Override
        public void setTransformerInfo(TransformerInfo transformerInfo) {
            // no-op
        }
    }

    /**
     * Dummy EntityUtils implementation.
     */
    private static class DummyEntityUtils extends EntityUtils {
        protected DummyEntityUtils(DSLContext dslContext) {
            super(dslContext);
        }

        @Override
        public Class<? extends CommonDataModel> getEntityClass(String entityName) {
            // Always return TestCommonDataModel for testing.
            return TestCommonDataModel.class;
        }
    }

    /**
     * A simple CommonDataModel implementation for testing.
     */
    @Setter
    @Getter
    public static class TestCommonDataModel extends CommonDataModel {
        private String value;

        public TestCommonDataModel() {
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public void setId(String id) {

        }

        @Override
        public Integer getVersion() {
            return Integer.valueOf(0);
        }

        @Override
        public void setVersion(Integer version) {

        }

        @Override
        public ActiveStatus getActiveStatus() {
            return null;
        }

        @Override
        public void setActiveStatus(ActiveStatus activeStatus) {

        }

        @Override
        public String getActiveStatusReason() {
            return "";
        }

        @Override
        public void setActiveStatusReason(String activeStatusReason) {

        }

        @Override
        public LocalDateTime getCreationTime() {
            return null;
        }

        @Override
        public void setCreationTime(LocalDateTime creationTime) {

        }

        @Override
        public LocalDateTime getLastModifiedTime() {
            return null;
        }

        @Override
        public void setLastModifiedTime(LocalDateTime lastModifiedTime) {

        }

        @Override
        public String getCreatedBy() {
            return "";
        }

        @Override
        public void setCreatedBy(String createdBy) {

        }

        @Override
        public String getModifiedBy() {
            return "";
        }

        @Override
        public void setModifiedBy(String modifiedBy) {

        }

        @Override
        public String getLob() {
            return "";
        }

        @Override
        public void setLob(String lob) {

        }

        @Override
        public String getSource() {
            return "";
        }

        @Override
        public void setSource(String source) {

        }

        @Override
        public JsonNode getExtendedAttributes() {
            return null;
        }

        @Override
        public void setExtendedAttributes(JsonNode extendedAttributes) {

        }

        @Override
        public String getHash() {
            return "";
        }

        @Override
        public void setHash(String hash) {

        }

    }
}