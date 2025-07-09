//package com.salescode.dis.insights.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
////@EnableWebMvc
//public class WebConfig implements WebMvcConfigurer {
//
//    // Keep the default value as "*"
//    @Value("${cors.allowed-origins:*}")
//    private String[] allowedOrigins;
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        // Start building the CORS registration for the /api/** path
//        CorsRegistration registration = registry.addMapping("/api/**")
//                // Always set allowedOrigins based on the @Value injection (defaults to "*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Specify allowed methods
//                .allowedHeaders("*") // Allow all headers
//                .maxAge(3600); // Cache preflight response for 1 hour
//    }
//}