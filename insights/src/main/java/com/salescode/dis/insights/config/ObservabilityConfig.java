package com.salescode.dis.insights.config;

import ai.salescode.observability.toolkit.api.AuthenticationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObservabilityConfig {

 @Bean
 @Primary
 public AuthenticationContext authenticationContext() {
 return new ServiceAuthenticationContext();
 }
}