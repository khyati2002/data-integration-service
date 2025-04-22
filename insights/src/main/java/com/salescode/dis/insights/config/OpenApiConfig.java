package com.salescode.dis.insights.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Job Management API", version = "v1", description = "API for creating, updating, and fetching jobs"))
public class OpenApiConfig {

}