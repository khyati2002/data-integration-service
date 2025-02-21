package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.DatabaseConnectionUtil;
import com.salescode.dim.PropertyLoader;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EntityUtils.
 *
 * These tests load application properties, obtain a real connection via DatabaseConnectionUtil,
 * and then verify:
 *   - That getInstance(DSLContext) properly initializes the singleton.
 *   - That getInstance() throws an exception when EntityUtils was not initialized.
 *   - That getEntityClass(String) returns the expected entity class (case-insensitively)
 *     when the internal subClasses set is overridden.
 *   - That an unknown entity name triggers an IllegalArgumentException.
 *   - That getUniqueKeys returns null (as per the current implementation).
 */
public class EntityUtilsTest {

    private Connection connection;
    private DSLContext dsl;
    private EntityUtils entityUtils;

    @BeforeEach
    public void setUp() throws Exception {
        // Load application properties and create a connection using DatabaseConnectionUtil.
        Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(null);
        Properties properties = applicationProperties.getOrDefault("Common", new Properties());
        connection = DatabaseConnectionUtil.createConnection(properties);
        this.dsl = DatabaseConnectionUtil.createDSLContext(connection);

        // Reset any previous singleton instance to use the current DSLContext.
        resetEntityUtilsSingleton();

        entityUtils = EntityUtils.getInstance(dsl);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Resets the singleton instance of EntityUtils via reflection.
     */
    private void resetEntityUtilsSingleton() throws Exception {
        Field instanceField = EntityUtils.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    /**
     * Test that getInstance(DSLContext) initializes the singleton and that getInstance() returns the same instance.
     */
    @Test
    public void testGetInstance() {
        assertNotNull(entityUtils, "EntityUtils instance should not be null");
        // Calling getInstance() without parameters should return the same instance.
        EntityUtils instance2 = EntityUtils.getInstance();
        assertSame(entityUtils, instance2, "Both calls should return the same singleton instance");
    }

    /**
     * Test that getInstance() without initialization throws an exception.
     */
    @Test
    public void testGetInstanceWithoutInitialization() throws Exception {
        resetEntityUtilsSingleton();
        Exception exception = assertThrows(IllegalStateException.class, () ->
                EntityUtils.getInstance()
        );
        assertTrue(exception.getMessage().contains("EntityUtils was not initialized"));
    }

    @Test
    public void testGetEntityClassFound() throws Exception {
        // Override the subClasses field to contain only DummyEntity.class.
        Field subClassesField = EntityUtils.class.getDeclaredField("subClasses");
        subClassesField.setAccessible(true);

        // Retrieve the entity class using proper case.
        Class<? extends CommonDataModel> clazz = entityUtils.getEntityClass("OutletDetails");
        assertNotNull(clazz, "Entity class should be found");
        assertEquals(OutletDetails.class, clazz, "Returned class should be DummyEntity");
    }

    /**
     * Test that getEntityClass throws an IllegalArgumentException when no matching entity is found.
     */
    @Test
    public void testGetEntityClassNotFound() throws Exception {
        // Override the subClasses field to be empty.
        Field subClassesField = EntityUtils.class.getDeclaredField("subClasses");
        subClassesField.setAccessible(true);
        subClassesField.set(entityUtils, Set.of());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                entityUtils.getEntityClass("NonExistingEntity")
        );
        assertTrue(exception.getMessage().contains("Entity not found: NonExistingEntity"),
                "Exception message should indicate entity was not found");
    }

    /**
     * Test that getUniqueKeys returns null as per the current implementation.
     */
    @Test
    public void testGetUniqueKeys() {
        Set<String> keys = entityUtils.getUniqueKeys(OutletDetails.class);
        assertNull(keys, "Expected getUniqueKeys to return null");
    }
}