package com.bcbs239.regtech.ingestion.application.service;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for applying rate limiting per bank to prevent abuse.
 * This is a simple in-memory implementation for development.
 * In production, this would use Redis or a distributed cache.
 */
@Service
public class RateLimitingService {
    
    private static final int MAX_UPLOADS_PER_HOUR = 10;
    private static final int MAX_UPLOADS_PER_DAY = 50;
    
    // In-memory storage for rate limiting (would be Redis in production)
    private final ConcurrentHashMap<String, RateLimitData> rateLimitData = new ConcurrentHashMap<>();
    
    /**
     * Check if the bank can upload a file based on rate limiting rules.
     */
    public Result<Void> checkRateLimit(BankId bankId) {
        String key = bankId.value();
        Instant now = Instant.now();
        
        RateLimitData data = rateLimitData.computeIfAbsent(key, k -> new RateLimitData());
        
        synchronized (data) {
            // Clean up old entries
            cleanupOldEntries(data, now);
            
            // Check hourly limit
            if (data.hourlyCount.get() >= MAX_UPLOADS_PER_HOUR) {
                return Result.failure(ErrorDetail.businessRuleViolation(
                    "RATE_LIMIT_EXCEEDED", 
                    String.format("Bank %s has exceeded the hourly upload limit of %d files", 
                        bankId.value(), MAX_UPLOADS_PER_HOUR)
                ));
            }
            
            // Check daily limit
            if (data.dailyCount.get() >= MAX_UPLOADS_PER_DAY) {
                return Result.failure(ErrorDetail.businessRuleViolation(
                    "RATE_LIMIT_EXCEEDED", 
                    String.format("Bank %s has exceeded the daily upload limit of %d files", 
                        bankId.value(), MAX_UPLOADS_PER_DAY)
                ));
            }
            
            // Increment counters
            data.hourlyCount.incrementAndGet();
            data.dailyCount.incrementAndGet();
            data.lastAccess = now;
            
            return Result.success(null);
        }
    }
    
    private void cleanupOldEntries(RateLimitData data, Instant now) {
        // Reset hourly counter if more than an hour has passed
        if (data.lastHourlyReset == null || 
            ChronoUnit.HOURS.between(data.lastHourlyReset, now) >= 1) {
            data.hourlyCount.set(0);
            data.lastHourlyReset = now;
        }
        
        // Reset daily counter if more than a day has passed
        if (data.lastDailyReset == null || 
            ChronoUnit.DAYS.between(data.lastDailyReset, now) >= 1) {
            data.dailyCount.set(0);
            data.lastDailyReset = now;
        }
    }
    
    /**
     * Get current rate limit status for a bank.
     */
    public RateLimitStatus getRateLimitStatus(BankId bankId) {
        String key = bankId.value();
        RateLimitData data = rateLimitData.get(key);
        
        if (data == null) {
            return new RateLimitStatus(0, 0, MAX_UPLOADS_PER_HOUR, MAX_UPLOADS_PER_DAY);
        }
        
        synchronized (data) {
            cleanupOldEntries(data, Instant.now());
            return new RateLimitStatus(
                data.hourlyCount.get(), 
                data.dailyCount.get(), 
                MAX_UPLOADS_PER_HOUR, 
                MAX_UPLOADS_PER_DAY
            );
        }
    }
    
    private static class RateLimitData {
        final AtomicInteger hourlyCount = new AtomicInteger(0);
        final AtomicInteger dailyCount = new AtomicInteger(0);
        volatile Instant lastAccess = Instant.now();
        volatile Instant lastHourlyReset = Instant.now();
        volatile Instant lastDailyReset = Instant.now();
    }
    
    public record RateLimitStatus(
        int currentHourlyCount,
        int currentDailyCount,
        int maxHourlyLimit,
        int maxDailyLimit
    ) {}
}