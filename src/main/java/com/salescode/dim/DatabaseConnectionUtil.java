package com.salescode.dim;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionUtil {

    private static HikariDataSource dataSource;

    /**
     * Creates a new database connection using the given properties.
     *
     * @param properties Database configuration properties.
     * @return A new Connection instance.
     * @throws SQLException If a database access error occurs.
     * @throws ClassNotFoundException If the JDBC driver class is not found.
     */
    public static Connection createConnection(Properties properties) throws SQLException, ClassNotFoundException {
        String jdbcUrl = properties.getProperty("jdbc.url");
        String jdbcUser = properties.getProperty("jdbc.user");
        String jdbcPassword = properties.getProperty("jdbc.password");
        String jdbcDriver = properties.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"); // Default to MySQL

        // Ensure required properties are provided
        if (jdbcUrl == null || jdbcUser == null || jdbcPassword == null) {
            throw new IllegalArgumentException("Missing database configuration properties.");
        }

        // Load the JDBC driver class
        Class.forName(jdbcDriver);

        // Return the database connection
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    /**
     * Creates a JOOQ DSLContext using the given database connection.
     *
     * @param connection Active database connection.
     * @return A JOOQ DSLContext instance.
     */
    public static DSLContext createDSLContext(Connection connection) {
        return DSL.using(connection, SQLDialect.MYSQL);
    }

    /**
     * Initializes the Hikari connection pool (up to 10 connections).
     * This should be called once at application startup.
     *
     * @param properties Database configuration properties.
     */
    public static synchronized void initConnectionPool(Properties properties) {
        if (dataSource != null) {
            // Already initialized, skip re-initializing
            return;
        }

        String jdbcUrl = properties.getProperty("jdbc.url");
        String jdbcUser = properties.getProperty("jdbc.user");
        String jdbcPassword = properties.getProperty("jdbc.password");
        String jdbcDriver = properties.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"); // Default to MySQL

        // Ensure required properties are provided
        if (jdbcUrl == null || jdbcUser == null || jdbcPassword == null) {
            throw new IllegalArgumentException("Missing database configuration properties.");
        }

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(jdbcUser);
        config.setPassword(jdbcPassword);
        config.setDriverClassName(jdbcDriver);

        // Pool size: maximum 10 connections
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
    }

    /**
     * Returns a pooled database connection from Hikari (up to 10 connections).
     * Make sure initConnectionPool() is called first.
     *
     * @return A Connection from HikariDataSource
     * @throws SQLException If a database access error occurs.
     */
    public static Connection getPooledConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Connection pool is not initialized. Call initConnectionPool() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Create a JOOQ DSLContext using a pooled connection.
     * Make sure initConnectionPool() is called before using this method.
     *
     * @return A DSLContext from the pooled connection
     * @throws SQLException If a database access error occurs.
     */
    public static DSLContext createPooledDSLContext() throws SQLException {
        return DSL.using(getPooledConnection(), SQLDialect.MYSQL);
    }
}