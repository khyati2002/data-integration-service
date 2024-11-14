package com.salescode.dis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${app.jobs.from-kafka-to-db.db.url}")
    private String url;

    @Value("${app.jobs.from-kafka-to-db.db.user}")
    private String user;

    @Value("${app.jobs.from-kafka-to-db.db.password}")
    private String password;

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");  // MySQL driver
        dataSourceBuilder.url(url); // MySQL JDBC URL
        dataSourceBuilder.username(user);                    // MySQL username
        dataSourceBuilder.password(password);                    // MySQL password
        return dataSourceBuilder.build();  // Return the DataSource bean
    }
}
