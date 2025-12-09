package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * LogoutCommandHandler - Handles user logout
 * 
 * Revokes refresh tokens and logs logout events
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 9.4
 */
@Component
@Slf4j
public class LogoutCommandHandler {
    private final IRefreshTokenRepository refreshTokenRepository;
    
    public LogoutCommandHandler(IRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    /**
     * Handles the logout command
     * 
     * This method implements a fail-safe approach: even if token revocation fails,
     * it still returns success to the client (Requirement 4.5)
     */
    @Transactional
    public Result<LogoutResponse> handle(LogoutCommand command) {
        try {
            // 1. Revoke all refresh tokens for the user (Requirement 4.1, 4.3)
            Result<Void> revokeResult = refreshTokenRepository.revokeAllForUser(
                command.userId()
            );
            
            if (revokeResult.isFailure()) {
                // Log error but continue with logout (Requirement 4.5)
                log.error("LOGOUT_REVOKE_FAILED - userId: {}, error: {}", 
                    command.userId().getValue(),
                    revokeResult.getError().get().getMessage());
                // Continue with logout even if revocation fails
            } else {
                log.debug("LOGOUT_TOKENS_REVOKED - userId: {}", 
                    command.userId().getValue());
            }
            
            // 2. Log logout event (Requirement 4.2, 9.4)
            log.info("USER_LOGGED_OUT - userId: {}, timestamp: {}", 
                command.userId().getValue(),
                Instant.now());
            
            // 3. Return success response (Requirement 4.4)
            return Result.success(LogoutResponse.success());
            
        } catch (Exception e) {
            // Log error but still return success to client (Requirement 4.5)
            log.error("LOGOUT_ERROR - userId: {}, error: {}", 
                command.userId().getValue(), 
                e.getMessage(), 
                e);
            
            // Return success even on error to ensure client-side logout completes
            return Result.success(LogoutResponse.success());
        }
    }
}
