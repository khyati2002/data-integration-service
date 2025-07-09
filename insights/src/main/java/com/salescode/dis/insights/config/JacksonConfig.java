package com.salescode.dis.insights.config;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.salescode.dis.insights.serializer.InstantToEpochDecimalSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class JacksonConfig {

    @Bean
    public Module instantEpochDecimalModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new InstantToEpochDecimalSerializer());
        return module;
    }
}
