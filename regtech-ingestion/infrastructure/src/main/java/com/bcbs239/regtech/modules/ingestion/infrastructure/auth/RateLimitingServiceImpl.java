package com.bcbs239.regtech.modules.ingestion.infrastructure.auth;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.application.batch.upload.UploadFileCommandHandler.RateLimitingService;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of rate limiting service.
 * Prevents abuse by limiting the number of uploads per bank within a time window.
 */
@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingServiceImpl.class);

    @Value("${ingestion.rate-limit.max-uploads-per-hour:10}")
    private int maxUploadsPerHour;

    @Value("${ingestion.rate-limit.time-window-minutes:60}")
    private int timeWindowMinutes;

    private final ConcurrentHashMap<String, RateLimitWindow> rateLimitWindows = new ConcurrentHashMap<>();

    @Override
    public Result<Void> checkRateLimit(BankId bankId) {
        log.debug("Checking rate limit for bank: {}", bankId.value());

        String bankIdValue = bankId.value();
        Instant now = Instant.now();
        
        // Get or create rate limit window for this bank
        RateLimitWindow window = rateLimitWindows.computeIfAbsent(bankIdValue, 
            k -> new RateLimitWindow(now));

        // Check if we need to reset the window
        if (window.shouldReset(now, timeWindowMinutes)) {
            window.reset(now);
            log.debug("Reset rate limit window for bank: {}", bankIdValue);
        }

        // Check current count against limit
        int currentCount = window.getCurrentCount();
        if (currentCount >= maxUploadsPerHour) {
            long resetTimeMinutes = window.getMinutesUntilReset(now, timeWindowMinutes);
            
            log.warn("Rate limit exceeded for bank: {} (count: {}, limit: {})", 
                bankIdValue, currentCount, maxUploadsPerHour);
            
            return Result.failure(new ErrorDetail("RATE_LIMIT_EXCEEDED", 
                String.format("Upload rate limit exceeded. Maximum %d uploads per %d minutes. " +
                    "Rate limit resets in %d minutes.", 
                    maxUploadsPerHour, timeWindowMinutes, resetTimeMinutes)));
        }

        // Increment counter
        window.increment();
        
        log.debug("Rate limit check passed for bank: {} (count: {}/{}, window: {})", 
            bankIdValue, currentCount + 1, maxUploadsPerHour, window.getWindowStart());

        return Result.success(null);
    }

    /**
     * Get current rate limit status for a bank (for monitoring/debugging).
     */
    public RateLimitStatus getRateLimitStatus(BankId bankId) {
        RateLimitWindow window = rateLimitWindows.get(bankId.value());
        if (window == null) {
            return new RateLimitStatus(0, maxUploadsPerHour, 0);
        }

        Instant now = Instant.now();
        if (window.shouldReset(now, timeWindowMinutes)) {
            return new RateLimitStatus(0, maxUploadsPerHour, 0);
        }

        return new RateLimitStatus(
            window.getCurrentCount(),
            maxUploadsPerHour,
            window.getMinutesUntilReset(now, timeWindowMinutes)
        );
    }

    /**
     * Clear rate limit for a bank (for admin operations).
     */
    public void clearRateLimit(BankId bankId) {
        rateLimitWindows.remove(bankId.value());
        log.info("Cleared rate limit for bank: {}", bankId.value());
    }

    private static class RateLimitWindow {
        private volatile Instant windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        public RateLimitWindow(Instant start) {
            this.windowStart = start;
        }

        public boolean shouldReset(Instant now, int windowMinutes) {
            return now.isAfter(windowStart.plus(windowMinutes, ChronoUnit.MINUTES));
        }

        public void reset(Instant now) {
            this.windowStart = now;
            this.count.set(0);
        }

        public int getCurrentCount() {
            return count.get();
        }

        public void increment() {
            count.incrementAndGet();
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public long getMinutesUntilReset(Instant now, int windowMinutes) {
            Instant resetTime = windowStart.plus(windowMinutes, ChronoUnit.MINUTES);
            return ChronoUnit.MINUTES.between(now, resetTime);
        }
    }

    public static class RateLimitStatus {
        private final int currentCount;
        private final int maxCount;
        private final long minutesUntilReset;

        public RateLimitStatus(int currentCount, int maxCount, long minutesUntilReset) {
            this.currentCount = currentCount;
            this.maxCount = maxCount;
            this.minutesUntilReset = minutesUntilReset;
        }

        public int getCurrentCount() { return currentCount; }
        public int getMaxCount() { return maxCount; }
        public long getMinutesUntilReset() { return minutesUntilReset; }
        public boolean isLimitExceeded() { return currentCount >= maxCount; }
    }
}