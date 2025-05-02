package com.salescode.dis.insights.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        System.out.println("CORS config applied");
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // matches your endpoints
                        .allowedOrigins("http://localhost:5174") // your frontend origin
                        .allowedMethods("*") // allows GET, POST, PUT, DELETE, etc.
                        .allowedHeaders("*"); // allows all headers
            }
        };
    }
}
