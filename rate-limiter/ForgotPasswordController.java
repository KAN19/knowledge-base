package com.example.ratelimiter.controller;

import com.example.ratelimiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    // You can inject either implementation depending on your choice:
    // 1. "bucket4jRateLimiter" (Token Bucket)
    // 2. "redisSlidingWindowRateLimiter" (Sliding Window Log)
    @Autowired
    @Qualifier("bucket4jRateLimiter")
    private RateLimiterService rateLimiterService;

    /**
     * Endpoint to trigger a forgot password email.
     * Enforces a rate limit (e.g., 5 requests per 24 hours).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> requestForgotPassword(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        // 1. Enforce rate limiting
        boolean allowed = rateLimiterService.tryConsume(email);
        if (!allowed) {
            response.put("status", "error");
            response.put("message", "Too many password reset requests. Please try again later.");
            response.put("remainingTokens", rateLimiterService.getRemainingTokens(email));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        // 2. Mock operations: Look up user, store request record, send email
        boolean emailExists = mockUserDatabaseLookup(email);
        
        // Security Best Practice: Even if the email doesn't exist, return a generic success 
        // message to prevent "email enumeration" (hackers figuring out who has accounts).
        if (emailExists) {
            mockStoreForgotPasswordRequestInDatabase(email);
            mockSendPasswordResetEmail(email);
        }

        response.put("status", "success");
        response.put("message", "If the email is registered, password reset instructions have been sent.");
        response.put("remainingTokens", rateLimiterService.getRemainingTokens(email));
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to dynamically change rate limit capacity at runtime.
     * Demonstrates dynamic configurations without code redeployment.
     */
    @PutMapping("/admin/rate-limit/config")
    public ResponseEntity<Map<String, String>> changeRateLimit(
            @RequestParam String email, 
            @RequestParam int newCapacity) {
        
        rateLimiterService.changeCapacity(email, newCapacity);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Capacity limit for " + email + " successfully updated to " + newCapacity);
        return ResponseEntity.ok(response);
    }

    // --- Mock Helper Methods for Demonstration ---

    private boolean mockUserDatabaseLookup(String email) {
        // Mock checking if the email exists in your Database (e.g., select * from users where email = ?)
        return true; 
    }

    private void mockStoreForgotPasswordRequestInDatabase(String email) {
        // Mock writing a record to your database (e.g., forgot_password_requests)
        System.out.println("[Database] Persisting request token and timestamp for: " + email);
    }

    private void mockSendPasswordResetEmail(String email) {
        // Mock calling JavaMailSender or third-party email API (e.g., SendGrid, AWS SES)
        System.out.println("[SMTP] Secure reset password email sent successfully to: " + email);
    }
}
