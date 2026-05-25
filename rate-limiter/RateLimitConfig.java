package com.example.ratelimiter.config;

import com.bucket4j.distributed.expiration.ExpirationAfterWriteStrategy;
import com.bucket4j.distributed.proxy.ProxyManager;
import com.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Set up a dedicated Lettuce RedisClient for Bucket4j.
     * Bucket4j's Redis-Lettuce extension requires direct access to Lettuce API.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        String userInfo = redisPassword.isEmpty() ? "" : ":" + redisPassword + "@";
        return RedisClient.create("redis://" + userInfo + redisHost + ":" + redisPort);
    }

    /**
     * The ProxyManager manages the life-cycle and distributed state of individual 
     * token buckets stored inside your Redis cache.
     */
    @Bean
    public ProxyManager<String> proxyManager(RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        // TTL strategy: If a rate limit key has not been modified/replenished 
                        // for 24 hours, automatically evict it from Redis to free memory.
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofDays(1)
                        )
                )
                .build();
    }

    /**
     * Standard Spring StringRedisTemplate. 
     * Used by the Sliding Window implementation to run custom Lua scripts.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
