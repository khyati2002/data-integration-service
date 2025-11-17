package com.salescode.dim.cache;

import com.applicate.services.channelkart.cache.DistributedCache;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.*;


public class CacheEvictionFunction extends RichFlatMapFunction<String, Void> {

        private transient DistributedCache distributedCache;
        private final Properties properties;
        @Override
        public void open(Configuration parameters) {
            this.distributedCache = DistributedCache.getInstance(properties);
        }

        public CacheEvictionFunction(Properties commonProperties) {
            this.properties = Objects.requireNonNull(commonProperties, "Properties cannot be null");
        }

        @Override
        public void flatMap(String cachePattern, Collector<Void> out) {
           DistributedCache.getInstance().getAllCachesAndClear(cachePattern,properties.getProperty("lob"));

        }

    }

