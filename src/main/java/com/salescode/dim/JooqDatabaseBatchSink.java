package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.esotericsoftware.minlog.Log;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JooqDatabaseBatchSink implements Sink<CommonDataModel> {

    private static final long serialVersionUID = 6676299950699299484L;
    private static final Logger LOG = LoggerFactory.getLogger(JooqDatabaseBatchSink.class);
    private final Properties properties;
    private final int batchSize;
    private final long batchIntervalMs;

    public JooqDatabaseBatchSink(Properties dbProperties) {
        this.properties = dbProperties;
        this.batchSize = Integer.parseInt(dbProperties.getProperty("batch.size", "100"));
        this.batchIntervalMs = Long.parseLong(dbProperties.getProperty("batch.interval.ms", "5000"));
    }

    @Override
    public SinkWriter<CommonDataModel> createWriter(InitContext context) {
        try {

            return new JooqDatabaseBatchSinkWriter(properties, batchSize, batchIntervalMs);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Error initializing JooqDatabaseBatchSink", e);
        }
    }

    // Inner class implementing SinkWriter for batch processing
    private static class JooqDatabaseBatchSinkWriter implements SinkWriter<CommonDataModel> {
        private final Connection connection;
        private final DSLContext dslContext;
        private final List<CommonDataModel> batchBuffer;
        private final int batchSize;
        private final long batchIntervalMs;
        private long lastBatchTime;

        public JooqDatabaseBatchSinkWriter(Properties properties, int batchSize, long batchIntervalMs) throws SQLException, ClassNotFoundException {
            this.connection = DatabaseConnectionUtil.createConnection(properties);
            this.dslContext = DatabaseConnectionUtil.createDSLContext(connection);
            this.batchBuffer = new ArrayList<>();
            this.batchSize = batchSize;
            this.batchIntervalMs = batchIntervalMs;
            this.lastBatchTime = System.currentTimeMillis();
        }

        @Override
        public void write(CommonDataModel value, Context context) throws IOException {
            try {
                LOG.info("Writing into sink");
//                batchBuffer.add(value);
//                long currentTime = System.currentTimeMillis();
//                if (batchBuffer.size() >= batchSize || (currentTime - lastBatchTime) >= batchIntervalMs) {
//                    flush(false);
//                    LOG.info("Flushing into sink");
//                    lastBatchTime = currentTime;
//                }
            } catch (Exception e) {
                throw new IOException("Failed to add record to batch", e);
            }
        }

        @Override
        public void flush(boolean endOfInput) throws IOException {
            LOG.info("Flushing {} records into sink...", batchBuffer.size());
            if (!batchBuffer.isEmpty()) {
                try {
                    dslContext.transaction(configuration -> {
//                        DSL.using(configuration)
//                           .batchInsert(batchBuffer.stream()
//                                                   .map(record -> DSL.using(configuration)
//                                                                     .newRecord(Tables.STREAMING_DATA, record.getId(), record.getPayload()))
//                                                   .toList()
//                           ).execute();
                    });
                    batchBuffer.clear();
                } catch (Exception e) {
                    throw new IOException("Batch insert failed", e);
                }
            }
        }

        @Override
        public void close() throws Exception {
            flush(true);
            if (connection != null) {
                connection.close();
            }
            LOG.info("Closed connection successfully");
        }
    }
}