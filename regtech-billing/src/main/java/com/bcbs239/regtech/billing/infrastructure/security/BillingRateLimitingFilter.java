package com.bcbs239.regtech.billing.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter for billing endpoints.
 * 
 * Implements rate limiting based on client IP address to prevent abuse
 * of payment processing and webhook endpoints. Uses a sliding window
 * approach with different limits for different endpoint types.
 */
public class BillingRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BillingRateLimitingFilter.class);

    private final int paymentRateLimit;
    private final int webhookRateLimit;
    private final int defaultRateLimit;
    
    // Rate limiting storage - in production this should be replaced with Redis
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitStore = new ConcurrentHashMap<>();

    public BillingRateLimitingFilter(int paymentRateLimit) {
        this.paymentRateLimit = paymentRateLimit;
        this.webhookRateLimit = paymentRateLimit * 10; // Webhooks can be more frequent
        this.defaultRateLimit = paymentRateLimit * 2;
    }

    public BillingRateLimitingFilter(int paymentRateLimit, int webhookRateLimit) {
        this.paymentRateLimit = paymentRateLimit;
        this.webhookRateLimit = webhookRateLimit;
        this.defaultRateLimit = paymentRateLimit * 2;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        // Determine rate limit based on endpoint
        int rateLimit = determineRateLimit(endpoint);
        
        // Skip rate limiting for health check endpoints
        if (endpoint.endsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Create rate limit key
        String rateLimitKey = clientIp + ":" + getEndpointCategory(endpoint);

        // Check and update rate limit
        if (isRateLimited(rateLimitKey, rateLimit)) {
            logger.warn("Rate limit exceeded for client {} on endpoint {}", clientIp, endpoint);
            
            response.setStatus(429);
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(getNextResetTime()));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        // Add rate limit headers to response
        RateLimitInfo rateLimitInfo = rateLimitStore.get(rateLimitKey);
        if (rateLimitInfo != null) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rateLimit - rateLimitInfo.getRequestCount())));
            response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitInfo.getResetTime()));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request should be rate limited
     */
    private boolean isRateLimited(String rateLimitKey, int rateLimit) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(1, ChronoUnit.MINUTES);

        rateLimitStore.compute(rateLimitKey, (key, existing) -> {
            if (existing == null || existing.getWindowStart().isBefore(windowStart)) {
                // Create new window
                return new RateLimitInfo(now, 1, now.plus(1, ChronoUnit.MINUTES).getEpochSecond());
            } else {
                // Update existing window
                existing.incrementRequestCount();
                return existing;
            }
        });

        RateLimitInfo rateLimitInfo = rateLimitStore.get(rateLimitKey);
        return rateLimitInfo.getRequestCount() > rateLimit;
    }

    /**
     * Determine rate limit based on endpoint
     */
    private int determineRateLimit(String endpoint) {
        if (endpoint.contains("/process-payment")) {
            return paymentRateLimit;
        } else if (endpoint.contains("/webhooks/")) {
            return webhookRateLimit;
        } else {
            return defaultRateLimit;
        }
    }

    /**
     * Get endpoint category for rate limiting
     */
    private String getEndpointCategory(String endpoint) {
        if (endpoint.contains("/process-payment")) {
            return "payment";
        } else if (endpoint.contains("/webhooks/")) {
            return "webhook";
        } else {
            return "billing";
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Get next reset time (start of next minute)
     */
    private long getNextResetTime() {
        return Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter billing endpoints
        return !request.getRequestURI().startsWith("/api/v1/billing/") && 
               !request.getRequestURI().startsWith("/api/v1/subscriptions/");
    }

    /**
     * Rate limit information for a specific client and endpoint category
     */
    private static class RateLimitInfo {
        private final Instant windowStart;
        private final AtomicInteger requestCount;
        private final long resetTime;

        public RateLimitInfo(Instant windowStart, int initialCount, long resetTime) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(initialCount);
            this.resetTime = resetTime;
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public int getRequestCount() {
            return requestCount.get();
        }

        public void incrementRequestCount() {
            requestCount.incrementAndGet();
        }

        public long getResetTime() {
            return resetTime;
        }
    }
}
