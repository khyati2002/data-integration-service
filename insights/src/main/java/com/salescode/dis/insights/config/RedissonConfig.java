//package com.salescode.dis.insights.config;
//
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.codec.JsonJacksonCodec;
//import org.redisson.config.Config;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RedissonConfig {
//
//    @Value("${cache.redisUrl}")
//    private String redisUrl;
//
//    @Bean(destroyMethod = "shutdown")
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.setCodec(new JsonJacksonCodec()); // Use JSON for readability and portability
//        config.useSingleServer().setAddress(redisUrl);
//        return Redisson.create(config);
//    }
//}
