package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NonNull;

import java.time.Instant;

/**
 * Presigned URL value object with expiration tracking
 * Represents a temporary authenticated URL for secure file access
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PresignedUrl(@NonNull String url, @NonNull Instant expiresAt, boolean valid) {
    
    /**
     * Compact constructor with validation
     */
    public PresignedUrl {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Presigned URL cannot be null or blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration time cannot be null");
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expiration time cannot be in the past");
        }
    }
    
    /**
     * Check if the URL has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the URL is still valid
     */
    public boolean isValid() {
        // derive validity from expiration time to avoid storing stale values
        return !isExpired();
    }
    
    /**
     * Get remaining time until expiration in seconds
     */
    public long getRemainingSeconds() {
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
    
    @Override
    public String toString() {
        return url;
    }
}
