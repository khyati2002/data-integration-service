package com.salescode.dim;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class DatabaseConnectionUtil {

    public static synchronized HikariDataSource initConnectionPool(Properties properties, int connectionCount) {
        log.info("Connection created successfully");
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
        config.setConnectionTimeout(10000);

        // Pool size: maximum 10 connections
        config.setMaximumPoolSize(connectionCount);

        return new HikariDataSource(config);
    }

    public static DSLContext createPooledDSLContext(DataSource dataSource) throws SQLException {
        return DSL.using(dataSource, SQLDialect.MYSQL);
    }
}