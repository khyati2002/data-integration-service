package com.salescode.jooq;

import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.meta.jaxb.Generate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.regex.Pattern;

@Configuration
public class JooqConfig {

    private static final String DEFAULT_SCHEMA = "ckroot";

    @Bean
    public org.jooq.Configuration jooqConfiguration(DataSource dataSource) {
        // Set up jOOQ settings with default schema and render mapping
        Settings settings = new Settings()
                .withRenderFormatted(true) // For better readability
                .withRenderSchema(true) // Ensure schema names are rendered
                .withRenderCatalog(false)
                .withRenderMapping(new RenderMapping()
                        .withSchemata(new MappedSchema()
//                                .withInput(null)
                                .withInputExpression(Pattern.compile(".*")) // Matches when no schema is specified
                                .withOutput(DEFAULT_SCHEMA))); // Maps to your default schema

        // Return a configuration set up for the default schema
        org.jooq.Configuration set = new DefaultConfiguration()
                .set(dataSource)
                .set(SQLDialect.MYSQL)
                .set(settings);
        return set;
    }

    @Bean
    public DSLContext dslContext(org.jooq.Configuration configuration) {
        // Create a DSLContext based on the jOOQ configuration bean
        return DSL.using(configuration);
    }
}