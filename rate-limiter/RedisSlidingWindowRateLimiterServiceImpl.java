package com.example.ratelimiter.service.impl;

import com.example.ratelimiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service("redisSlidingWindowRateLimiter")
public class RedisSlidingWindowRateLimiterServiceImpl implements RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int DEFAULT_LIMIT = 5;
    private static final long WINDOW_MS = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds

    // Store custom capacity dynamically per key at runtime
    private final ConcurrentHashMap<String, Integer> customCapacities = new ConcurrentHashMap<>();

    // Lua script executing ZREMRANGEBYSCORE, ZCARD, ZADD, and EXPIRE atomically
    private static final String LUA_RATE_LIMITER = 
            "local key = KEYS[1]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local limit = tonumber(ARGV[3])\n" +
            "local member = ARGV[4]\n" +
            "\n" +
            "-- Remove timestamps older than current window bounds\n" +
            "redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)\n" +
            "\n" +
            "-- Count elements remaining inside the window\n" +
            "local current_requests = redis.call('ZCARD', key)\n" +
            "\n" +
            "if current_requests < limit then\n" +
            "    -- Consume token: Add member, score is the current timestamp\n" +
            "    redis.call('ZADD', key, now, member)\n" +
            "    -- Set dynamic expiration to ensure cleanup of inactive keys\n" +
            "    redis.call('EXPIRE', key, math.ceil(window / 1000))\n" +
            "    return 1\n" +
            "else\n" +
            "    -- Rate limited\n" +
            "    return 0\n" +
            "end;";

    private final RedisScript<Long> rateLimitScript = new DefaultRedisScript<>(LUA_RATE_LIMITER, Long.class);

    @Override
    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    @Override
    public boolean tryConsume(String key, int tokens) {
        // Sliding window usually processes events individually. 
        // We run the Lua script for each token requested.
        int limit = customCapacities.getOrDefault(key, DEFAULT_LIMIT);
        String redisKey = "rate_limit:sliding:" + key;
        long now = System.currentTimeMillis();
        String uniqueMember = now + ":" + UUID.randomUUID().toString();

        for (int i = 0; i < tokens; i++) {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(now),
                    String.valueOf(WINDOW_MS),
                    String.valueOf(limit),
                    uniqueMember
            );
            
            if (result == null || result == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void changeCapacity(String key, int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        // Changing threshold is simple: just save the new limit parameter. 
        // Redis data structure only holds the timestamps, which remain unchanged.
        customCapacities.put(key, newCapacity);
    }

    @Override
    public long getRemainingTokens(String key) {
        String redisKey = "rate_limit:sliding:" + key;
        long now = System.currentTimeMillis();
        
        // Remove expired requests to ensure accurate count
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, now - WINDOW_MS);
        
        Long currentCount = redisTemplate.opsForZSet().zCard(redisKey);
        long count = currentCount != null ? currentCount : 0L;
        
        int limit = customCapacities.getOrDefault(key, DEFAULT_LIMIT);
        return Math.max(0, limit - count);
    }
}
