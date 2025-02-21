package com.salescode.dim;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionUtil {

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

}