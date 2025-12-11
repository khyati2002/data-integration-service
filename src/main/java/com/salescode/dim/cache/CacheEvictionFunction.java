package com.salescode.dim.cache;

import com.salescode.dim.cache.CacheManager;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import java.util.stream.Collectors;


public class CacheEvictionFunction extends RichFlatMapFunction<String, Void> {

        private transient CacheManager cacheManager;
        private final Properties properties;
        @Override
        public void open(Configuration parameters) {
            // Initialize CacheManager before processing records
            this.cacheManager = CacheManager.getInstance(properties);
        }

        public CacheEvictionFunction(Properties commonProperties) {
            this.properties = Objects.requireNonNull(commonProperties, "Properties cannot be null");
        }


        @Override
        public void flatMap(String cachePattern, Collector<Void> out) {
            CacheManager.getInstance().getAllCachesAndClear(cachePattern,properties.getProperty("lob"));

        }

    }

