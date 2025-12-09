package com.bcbs239.regtech.iam.infrastructure.scheduling;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled task to clean up expired refresh tokens
 * Runs daily at 2 AM (configurable via cron expression)
 * Deletes tokens that expired more than 30 days ago (configurable)
 * 
 * Requirements: 6.5
 */
@Component
@ConditionalOnProperty(
    prefix = "iam.token-cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RefreshTokenCleanupScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);
    
    private final IRefreshTokenRepository refreshTokenRepository;
    private final IAMProperties iamProperties;
    
    public RefreshTokenCleanupScheduler(
        IRefreshTokenRepository refreshTokenRepository,
        IAMProperties iamProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.iamProperties = iamProperties;
    }
    
    /**
     * Scheduled cleanup job for expired refresh tokens
     * Runs according to the cron expression configured in application-iam.yml
     * Default: Daily at 2 AM (0 0 2 * * ?)
     */
    @Scheduled(cron = "${iam.token-cleanup.cron:0 0 2 * * ?}")
    public void cleanupExpiredTokens() {
        Instant startTime = Instant.now();
        
        log.info("TOKEN_CLEANUP_STARTED - timestamp: {}, retentionDays: {}", 
            startTime,
            iamProperties.getTokenCleanup().getRetentionDays());
        
        try {
            // Calculate cutoff date: delete tokens expired more than N days ago
            int retentionDays = iamProperties.getTokenCleanup().getRetentionDays();
            Instant cutoffDate = Instant.now().minus(Duration.ofDays(retentionDays));
            
            // Delete expired tokens
            Result<Void> result = refreshTokenRepository.deleteExpiredTokens(cutoffDate);
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            if (result.isSuccess()) {
                log.info("TOKEN_CLEANUP_COMPLETED - cutoffDate: {}, duration: {}ms", 
                    cutoffDate,
                    duration.toMillis());
            } else {
                log.error("TOKEN_CLEANUP_FAILED - cutoffDate: {}, duration: {}ms, error: {}", 
                    cutoffDate,
                    duration.toMillis(),
                    result.getError().get().getMessage());
            }
        } catch (Exception e) {
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            log.error("TOKEN_CLEANUP_ERROR - duration: {}ms, error: {}", 
                duration.toMillis(),
                e.getMessage(),
                e);
        }
    }
}
