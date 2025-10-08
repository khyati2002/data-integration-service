package com.salescode.dis.insights.config;

import com.salescode.auth.sdk.filters.cache.NoOpAuthCacheClient;
import com.salescode.auth.sdk.filters.requests.SalesCodeAuthFilter;
import com.salescode.auth.sdk.filters.requests.SalesCodeAuthManager;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@Configuration
public class HttpSecurityConfiguration {

    @Bean
    public SalesCodeAuthManager salesCodeAuthManager() {
        return SalesCodeAuthFilter.builder("sample-service")
                .withDefaultEnvironment("uat")
                .withCacheClient(new NoOpAuthCacheClient())
                .withLocalCredentials()
                .withEnvironments("demo", "prod", "dev", "local")
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SalesCodeAuthManager salesCodeAuthManager,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/hckeck","/status","/api/properties/**","/api/modes","/api/integration-stats","/api/sse/health", "/api/sse/status", "/api/*/master/*/unit/*/progress").permitAll()
                        .anyRequest().authenticated()
                ).securityContext(context -> context.requireExplicitSave(false)
                );
        return salesCodeAuthManager.build(http);
    }

    @Bean
    public AuthenticationProvider provider(SalesCodeAuthManager authManager) {
        return authManager.provider();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // You can inject this from properties if needed
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false); // Set to true if you're using cookies/auth headers
        configuration.setMaxAge(3600L); // Cache duration for preflight requests

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web-> web.ignoring().requestMatchers(request-> request.getDispatcherType() == DispatcherType.ASYNC);
    }
}
