# Distributed Rate Limiter Implementation for Spring Boot

This directory contains the fully working Java classes and configurations to implement two rate-limiting strategies in your Spring Boot application:
1. **Token Bucket** (using Bucket4j + Redis)
2. **Sliding Window Log** (using Redis ZSET via Spring Boot's standard `StringRedisTemplate`)

---

## 📋 Directory Contents

*   **[rate_limiting_design_document.md](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/rate_limiting_design_document.md)**: The comprehensive system design document comparing standard DB limits, Redis Sliding Window, and Token Bucket (Bucket4j) algorithms with dynamic config strategies.
*   **[PessimisticLockingGuide.md](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/PessimisticLockingGuide.md)**: Standard guide to implementing database-backed pessimistic locking (`FOR UPDATE`) with timeout parameters in Spring Data JPA.
*   **[RateLimitConfig.java](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/RateLimitConfig.java)**: Sets up the Redis Lettuce connection and Bucket4j `ProxyManager` beans.
*   **[RateLimiterService.java](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/RateLimiterService.java)**: A clean Java interface outlining the rate-limiting contracts.
*   **[Bucket4jRateLimiterServiceImpl.java](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/Bucket4jRateLimiterServiceImpl.java)**: Implements the Token Bucket algorithm with dynamic capacity swap support.
*   **[RedisSlidingWindowRateLimiterServiceImpl.java](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/RedisSlidingWindowRateLimiterServiceImpl.java)**: Implements the dynamic sliding window logic via a Lua script.
*   **[ForgotPasswordController.java](file:///Users/nguyenanhkiet/Documents/synced-2nd-brain/inbox/rate-limiter/ForgotPasswordController.java)**: A REST Controller showing how to call the limiters, change capacities at runtime, and securely process password resets.

---

## 🛠️ Setup & Dependencies

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Starter Data Redis (includes Lettuce client) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Bucket4j Core -->
    <dependency>
        <groupId>com.bucket4j</groupId>
        <artifactId>bucket4j-core</artifactId>
        <version>8.11.1</version>
    </dependency>

    <!-- Bucket4j Redis Lettuce Integration -->
    <dependency>
        <groupId>com.bucket4j</groupId>
        <artifactId>bucket4j-redis</artifactId>
        <version>8.11.1</version>
    </dependency>
</dependencies>
```

Configure your Redis connection details in `application.properties` or `application.yml`:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
# Add spring.data.redis.password if your Redis instance is protected
```

---

## 🚀 How to Drag & Drop into Your Project

1. Copy **`RateLimitConfig.java`** into your project's configuration package.
2. Copy the **`RateLimiterService`** interface and its two implementations into your services package.
3. Update the package names (`package com.example...`) at the top of each file to match your project's package structure.
4. Inject your preferred implementation (e.g., `@Qualifier("bucket4jRateLimiter")`) into your controllers or business logic classes as shown in **`ForgotPasswordController.java`**.
