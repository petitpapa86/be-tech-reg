package com.bcbs239.regtech.iam.infrastructure.scheduling;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RefreshTokenCleanupScheduler
 * Tests the scheduled cleanup of expired refresh tokens
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {
    
    @Mock
    private IRefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private IAMProperties iamProperties;
    
    @Mock
    private IAMProperties.TokenCleanupProperties tokenCleanupProperties;
    
    private RefreshTokenCleanupScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        when(iamProperties.getTokenCleanup()).thenReturn(tokenCleanupProperties);
        when(tokenCleanupProperties.getRetentionDays()).thenReturn(30);
        
        scheduler = new RefreshTokenCleanupScheduler(refreshTokenRepository, iamProperties);
    }
    
    @Test
    void cleanupExpiredTokens_shouldDeleteTokensOlderThanRetentionPeriod() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
            .thenReturn(Result.success(null));
        
        // Act
        scheduler.cleanupExpiredTokens();
        
        // Assert
        ArgumentCaptor<Instant> cutoffDateCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteExpiredTokens(cutoffDateCaptor.capture());
        
        Instant cutoffDate = cutoffDateCaptor.getValue();
        Instant expectedCutoff = Instant.now().minus(Duration.ofDays(30));
        
        // Verify cutoff date is approximately 30 days ago (within 1 second tolerance)
        assertThat(cutoffDate).isBetween(
            expectedCutoff.minusSeconds(1),
            expectedCutoff.plusSeconds(1)
        );
    }
    
    @Test
    void cleanupExpiredTokens_shouldUseConfiguredRetentionDays() {
        // Arrange
        when(tokenCleanupProperties.getRetentionDays()).thenReturn(7);
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
            .thenReturn(Result.success(null));
        
        // Act
        scheduler.cleanupExpiredTokens();
        
        // Assert
        ArgumentCaptor<Instant> cutoffDateCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteExpiredTokens(cutoffDateCaptor.capture());
        
        Instant cutoffDate = cutoffDateCaptor.getValue();
        Instant expectedCutoff = Instant.now().minus(Duration.ofDays(7));
        
        // Verify cutoff date is approximately 7 days ago (within 1 second tolerance)
        assertThat(cutoffDate).isBetween(
            expectedCutoff.minusSeconds(1),
            expectedCutoff.plusSeconds(1)
        );
    }
    
    @Test
    void cleanupExpiredTokens_shouldHandleRepositoryFailureGracefully() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
            .thenReturn(Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                "DELETE_FAILED",
                com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                "Failed to delete expired tokens",
                "refresh_token.delete.failed"
            )));
        
        // Act - should not throw exception
        scheduler.cleanupExpiredTokens();
        
        // Assert
        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }
    
    @Test
    void cleanupExpiredTokens_shouldHandleExceptionGracefully() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act - should not throw exception
        scheduler.cleanupExpiredTokens();
        
        // Assert
        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }
}
