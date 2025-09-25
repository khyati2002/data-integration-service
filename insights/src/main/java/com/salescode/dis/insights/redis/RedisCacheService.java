package com.salescode.dis.insights.redis;

import com.salescode.dis.insights.enums.ProgressStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "eventId:";

    public boolean checkAndCacheEventId(String eventId, ProgressStage stageType) {
        String key = PREFIX + eventId + stageType;
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.MINUTES);
        return Boolean.FALSE.equals(wasSet);
    }
}

