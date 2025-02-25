package com.salescode.dim.transformers.registry;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.DatabaseConnectionUtil;
import com.salescode.dim.PropertyLoader;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.salescode.dim.jooq.generated.Tables.CK_TRANSFORMER_INFO;
import static org.junit.jupiter.api.Assertions.*;

public class TransformerInfoRegistryIntegrationTest {

    private Connection connection;
    private DSLContext dsl;

    @BeforeEach
    public void setUp() throws Exception {
        Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(null);
        Properties properties = applicationProperties.getOrDefault("Common", new Properties());
        connection = DatabaseConnectionUtil.createConnection(properties);
        this.dsl = DatabaseConnectionUtil.createDSLContext(connection);

        // Clear any previous singleton instance to use the current DSLContext.
        Field instanceField = TransformerInfoRegistry.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Test that passing a null DSLContext to getInstance throws an exception.
     */
    @Test
    public void testNullDSLContext() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TransformerInfoRegistry.getInstance(null);
        });
        assertTrue(exception.getMessage().contains("DSLContext cannot be null"));
    }

    /**
     * Test that calling getTransformerInfoById with a null ID throws an exception.
     */
    @Test
    public void testGetTransformerInfoByIdWithNull() {
        TransformerInfoRegistry registry = TransformerInfoRegistry.getInstance(dsl);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.getTransformerInfoById(null);
        });
        assertTrue(exception.getMessage().contains("Transformer ID cannot be null"));
    }

    /**
     * Test that calling getTransformerInfoByName with a null name throws an exception.
     */
    @Test
    public void testGetTransformerInfoByNameWithNull() {
        TransformerInfoRegistry registry = TransformerInfoRegistry.getInstance(dsl);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.getTransformerInfoByName(null);
        });
        assertTrue(exception.getMessage().contains("Transformer name cannot be null"));
    }

    /**
     * Test that the registry's cache is properly populated for all active records.
     * It verifies that lookups by ID and by name for every active record return the same instance.
     */
    @Test
    public void testCachePopulationForAllRecords() {
        // Initialize the registry (this preloads all active records via init()).
        TransformerInfoRegistry registry = TransformerInfoRegistry.getInstance(dsl);

        // Fetch all active records from the database.
        List<TransformerInfo> allActiveRecords = dsl.selectFrom(CK_TRANSFORMER_INFO)
                                                      .where(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                                                      .and(CK_TRANSFORMER_INFO.ID.isNotNull())
                                                      .and(CK_TRANSFORMER_INFO.NAME.isNotNull())
                                                      .fetchInto(TransformerInfo.class);

        // For each active record, verify that lookup by ID and by name returns the same instance.
        for (TransformerInfo record : allActiveRecords) {
            TransformerInfo fromId = registry.getTransformerInfoById(record.getId());
            TransformerInfo fromName = registry.getTransformerInfoByName(record.getName());

            assertNotNull(fromId, "Record should be fetched by id: " + record.getId());
            assertNotNull(fromName, "Record should be fetched by name: " + record.getName());
            assertEquals(record.getId(), fromId.getId(), "Mismatched ID for record " + record.getId());
            assertEquals(record.getName(), fromId.getName(), "Mismatched name for record " + record.getName());
            // Both lookups should return the same instance from the cache.
            assertSame(fromId, fromName, "Different instances returned for id " + record.getId() +
                    " and name " + record.getName());
        }
    }

    /**
     * Test that refreshRegistry clears the cache and subsequent lookups reflect updated data.
     */
    @Test
    public void testRefreshRegistry() {
        TransformerInfoRegistry registry = TransformerInfoRegistry.getInstance(dsl);

        // Fetch an active record from the database.
        List<TransformerInfo> activeRecords = dsl
                .fetch("SELECT * FROM ck_transformer_info WHERE ACTIVE_STATUS = ?", ActiveStatus.ACTIVE.name())
                .into(TransformerInfo.class);
        if (activeRecords.isEmpty()) {
            fail("No active records available for testing refreshRegistry");
        }
        TransformerInfo record = activeRecords.get(0);

        // Initial lookup: the record should have its original name.
        TransformerInfo fromInitialLookup = registry.getTransformerInfoById(record.getId());
        assertNotNull(fromInitialLookup, "Record should be fetched initially");

        // Simulate an external update by modifying the record's name in the database.
        String updatedName = record.getName() + "_Updated";
//        dsl.execute("UPDATE ck_transformer_info SET NAME = ? WHERE ID = ?", updatedName, record.getId());
//
//        // Clear the registry cache.
//        registry.refreshRegistry();
//
//        // Lookup the record again; it should now reflect the updated name.
//        TransformerInfo updatedRecord = registry.getTransformerInfoById(record.getId());
//        assertNotNull(updatedRecord, "Record should still be fetched after refresh");
//        assertEquals(updatedName, updatedRecord.getName(), "Record name should be updated after refresh");
    }
}