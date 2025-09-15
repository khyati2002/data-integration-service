package com.salescode.dis.insights.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LOCK_PREFIX = "file-processing-lock:";
    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 120; // 2 minutes
    private static final int DEFAULT_MAX_WAIT_SECONDS = 30; // Wait up to 30 seconds
    private static final int DEFAULT_RETRY_DELAY_MILLIS = 100; // 100ms between retries

    public boolean tryLock(String fileId, int timeoutSeconds) {
        String lockKey = LOCK_PREFIX + fileId;
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(timeoutSeconds));

        if (Boolean.TRUE.equals(success)) {
            log.debug("Acquired lock for fileId: {}", fileId);
            return true;
        }

        log.debug("Failed to acquire lock for fileId: {} (already locked)", fileId);
        return false;
    }

    /**
     * Tries to acquire lock with retry and wait
     */
    public boolean tryLockWithWait(String fileId, int maxWaitSeconds, int retryDelayMillis) {
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitSeconds * 1000L;

        while (true) {
            if (tryLock(fileId, DEFAULT_LOCK_TIMEOUT_SECONDS)) {
                return true;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= maxWaitMillis) {
                log.warn("Timeout waiting for lock on fileId: {} after {}ms", fileId, elapsed);
                return false;
            }

            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for lock on fileId: {}", fileId);
                return false;
            }
        }
    }

    public void releaseLock(String fileId) {
        String lockKey = LOCK_PREFIX + fileId;
        redisTemplate.delete(lockKey);
        log.debug("Released lock for fileId: {}", fileId);
    }

    public boolean executeWithLockAndWait(String fileId, Runnable action) {
        return executeWithLockAndWait(fileId, action, DEFAULT_MAX_WAIT_SECONDS, DEFAULT_RETRY_DELAY_MILLIS);
    }

    public boolean executeWithLockAndWait(String fileId, Runnable action, int maxWaitSeconds, int retryDelayMillis) {
        if (tryLockWithWait(fileId, maxWaitSeconds, retryDelayMillis)) {
            try {
                action.run();
                return true;
            } finally {
                releaseLock(fileId);
            }
        } else {
            log.error("Could not acquire lock for fileId: {} within {} seconds", fileId, maxWaitSeconds);
            return false;
        }
    }
}
