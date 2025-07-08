package com.salescode.dis.insights.config;

import com.salescode.auth.sdk.filters.cache.NoOpAuthCacheClient;
import com.salescode.auth.sdk.filters.requests.SalesCodeAuthFilter;
import com.salescode.auth.sdk.filters.requests.SalesCodeAuthManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class HttpSecurityConfiguration {

    @Bean
    public SalesCodeAuthManager salesCodeAuthManager() {
        return SalesCodeAuthFilter.builder("sample-service")
                .withDefaultEnvironment("uat")
                .withCacheClient(new NoOpAuthCacheClient())
//                .withCacheClient(new NoOpAuthCacheClient())
                .withLocalCredentials()
                .withEnvironments("demo", "prod", "dev", "local")
//                .withRedisConfiguration(RedisConfiguratioation.singleNode("127.0.0.1:6379"))
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SalesCodeAuthManager salesCodeAuthManager) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/hckeck", "/status").permitAll()
                        .anyRequest().authenticated()
                );
        return salesCodeAuthManager.build(http);
    }

    @Bean
    public AuthenticationProvider provider(SalesCodeAuthManager authManager) {
        return authManager.provider();
    }

}
