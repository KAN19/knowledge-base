package com.example.ratelimiter.service;

public interface RateLimiterService {

    /**
     * Attempts to consume 1 token for a given key identifier.
     *
     * @param key The unique identifier (e.g., user email, client IP, or API token).
     * @return true if token is successfully consumed (request allowed), false otherwise.
     */
    boolean tryConsume(String key);

    /**
     * Attempts to consume a specified number of tokens for a given key identifier.
     *
     * @param key    The unique identifier.
     * @param tokens Number of tokens to consume.
     * @return true if tokens are successfully consumed, false otherwise.
     */
    boolean tryConsume(String key, int tokens);

    /**
     * Updates the maximum capacity of a specific bucket dynamically at runtime.
     *
     * @param key         The unique identifier.
     * @param newCapacity The new maximum capacity limit.
     */
    void changeCapacity(String key, int newCapacity);

    /**
     * Returns the remaining tokens available in the bucket or window for this key.
     * Note: For sliding window log, this represents the capacity minus the count in active window.
     *
     * @param key The unique identifier.
     * @return The number of tokens currently remaining.
     */
    long getRemainingTokens(String key);
}
