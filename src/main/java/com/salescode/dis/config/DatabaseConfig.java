package com.salescode.dis.config;

import com.applicate.services.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(user);
        hikariDataSource.setPassword(password);
        hikariDataSource.setMaximumPoolSize(10);
        hikariDataSource.setMinimumIdle(1);
        hikariDataSource.setConnectionTimeout(120000);
        DatabaseProfileRegistry.setDefaultDs(hikariDataSource);
        return hikariDataSource;
    }
}
