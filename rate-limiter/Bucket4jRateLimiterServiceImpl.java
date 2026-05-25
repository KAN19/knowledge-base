package com.example.ratelimiter.service.impl;

import com.bucket4j.Bucket;
import com.bucket4j.BucketConfiguration;
import com.bucket4j.TokensInheritanceStrategy;
import com.bucket4j.distributed.proxy.ProxyManager;
import com.example.ratelimiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service("bucket4jRateLimiter")
public class Bucket4jRateLimiterServiceImpl implements RateLimiterService {

    @Autowired
    private ProxyManager<String> proxyManager;

    private static final int DEFAULT_CAPACITY = 5;
    private static final Duration WINDOW_DURATION = Duration.ofDays(1); // 24 hours

    /**
     * Builds the default token bucket configuration: 
     * Maximum of 5 tokens, refilled steadily at 1 token every 4.8 hours.
     */
    private final Supplier<BucketConfiguration> defaultBucketConfig = () -> createConfiguration(DEFAULT_CAPACITY);

    @Override
    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    @Override
    public boolean tryConsume(String key, int tokens) {
        Bucket bucket = getOrCreateBucket(key);
        return bucket.tryConsume(tokens);
    }

    @Override
    public void changeCapacity(String key, int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        Bucket bucket = getOrCreateBucket(key);
        BucketConfiguration newConfig = createConfiguration(newCapacity);
        
        // Dynamically replace old config with new config in Redis.
        // PROPORTIONALLY strategy: scales remaining tokens based on ratio (e.g. 2/5 -> 4/10)
        bucket.replaceConfiguration(newConfig, TokensInheritanceStrategy.PROPORTIONALLY);
    }

    @Override
    public long getRemainingTokens(String key) {
        Bucket bucket = getOrCreateBucket(key);
        return bucket.getAvailableTokens();
    }

    private Bucket getOrCreateBucket(String key) {
        String redisKey = "rate_limit:forgot_password:" + key;
        return proxyManager.builder().build(redisKey, defaultBucketConfig);
    }

    /**
     * Creates a Bucket4j configuration where tokens are refilled greedily 
     * at a rate matching capacity over a 24-hour window.
     */
    private BucketConfiguration createConfiguration(int capacity) {
        // Calculate the refill rate based on capacity over 24 hours.
        // e.g. 5 tokens in 24 hours => 1 token every 4.8 hours (288 minutes).
        Duration refillPeriod = Duration.ofMillis(WINDOW_DURATION.toMillis() / capacity);
        
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(1, refillPeriod))
                .build();
    }
}
