package com.salescode.dis.insights.service;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedissonClient redissonClient;

    @Autowired
    public RedisService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    long ttlMinutes = 10;
    // Save a string with TTL
    public void saveFileId(String lob, String masterName, String fileId, long ttlSeconds) {
        String key = buildKey(lob, masterName);
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(fileId, ttlSeconds, TimeUnit.MINUTES); // TTL set
    }

    public String getFileIdAndRefreshTtl(String lob, String masterName) {
        String key = buildKey(lob, masterName);
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();
        if (value != null) {
            bucket.expire(ttlMinutes, TimeUnit.MINUTES); // Reset TTL manually
        }
        return value;
    }

    private String buildKey(String lob, String masterName) {
        return "file:" + lob + ":" + masterName;
    }
}
