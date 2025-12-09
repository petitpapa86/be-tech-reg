package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.*;
import com.bcbs239.regtech.iam.domain.users.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * RefreshTokenCommandHandler - Handles token refresh operations
 * 
 * Validates refresh tokens, implements token rotation, and maintains tenant context
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 9.3
 */
@Component
@Slf4j
public class RefreshTokenCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final String jwtSecret;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    
    public RefreshTokenCommandHandler(
        UserRepository userRepository,
        IRefreshTokenRepository refreshTokenRepository,
        PasswordHasher passwordHasher,
        @Value("${iam.security.jwt.secret}") String jwtSecret,
        @Value("${iam.security.jwt.access-token-expiration-minutes:15}") int accessTokenExpirationMinutes,
        @Value("${iam.security.jwt.refresh-token-expiration-days:7}") int refreshTokenExpirationDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtSecret = jwtSecret;
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes);
        this.refreshTokenExpiration = Duration.ofDays(refreshTokenExpirationDays);
    }
    
    /**
     * Handles the refresh token command
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
     */
    @Transactional
    public Result<RefreshTokenResponse> handle(RefreshTokenCommand command) {
        // 1. Create a deterministic hash of the provided token for lookup
        // We use SHA-256 instead of BCrypt because BCrypt is salted and produces
        // different hashes for the same input, making lookups impossible
        String tokenHash = createDeterministicHash(command.refreshToken());
        
    
        // 2. Find refresh token in database (Requirement 2.1, 6.3)
        Maybe<RefreshToken> tokenMaybe = refreshTokenRepository.findByTokenHash(tokenHash);
        
        if (tokenMaybe.isEmpty()) {
           
            // Return generic error (Requirement 2.3, 8.1)
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid or expired refresh token",
                "refresh_token.invalid"
            ));
        }
        
        RefreshToken refreshToken = tokenMaybe.getValue();
        
        // 3. Validate token (Requirement 2.2, 2.3)
        if (!refreshToken.isValid()) {
            log.warn("REFRESH_TOKEN_INVALID - tokenId: {}, userId: {}, revoked: {}, expired: {}", 
                refreshToken.getId().value().toString(),
                refreshToken.getUserId().getValue(),
                refreshToken.isRevoked(),
                refreshToken.isExpired());
            // Return generic error (Requirement 2.2, 2.3, 8.1)
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid or expired refresh token",
                "refresh_token.invalid"
            ));
        }
        
        // 4. Load user (Requirement 2.1)
        Maybe<User> userMaybe = userRepository.userLoader(refreshToken.getUserId());
        
        if (userMaybe.isEmpty()) {
            log.error("REFRESH_TOKEN_USER_NOT_FOUND - userId: {}", 
                refreshToken.getUserId().getValue());
            return Result.failure(ErrorDetail.of(
                "USER_NOT_FOUND",
                ErrorType.NOT_FOUND_ERROR,
                "User not found",
                "user.not_found"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 5. Revoke old refresh token (token rotation) (Requirement 2.4, 6.2)
        Result<Void> revokeResult = refreshToken.revoke();
        if (revokeResult.isFailure()) {
            log.error("REFRESH_TOKEN_REVOKE_FAILED - tokenId: {}, error: {}", 
                refreshToken.getId().value().toString(),
                revokeResult.getError().get().getMessage());
            return Result.failure(revokeResult.getError().get());
        }
        
        // Save the revoked token
        refreshTokenRepository.save(refreshToken);
        
        // 6. Generate new token pair (Requirement 2.1, 5.1, 5.2)
        Result<TokenPair> tokenPairResult = TokenPair.create(
            user,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration,
            passwordHasher
        );
        
        if (tokenPairResult.isFailure()) {
            log.error("REFRESH_TOKEN_GENERATION_FAILED - userId: {}, error: {}", 
                user.getId().getValue(),
                tokenPairResult.getError().get().getMessage());
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 7. Save new refresh token (Requirement 6.1)
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            log.error("REFRESH_TOKEN_SAVE_FAILED - userId: {}, error: {}", 
                user.getId().getValue(),
                saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }
        
        // 8. Extract and maintain current tenant context (Requirement 2.5, 10.2)
        Maybe<TenantContextDto> tenantContext = extractTenantContext(user);
        
        // 9. Log token refresh (Requirement 9.3)
        log.info("TOKEN_REFRESHED - userId: {}, oldTokenId: {}, newTokenId: {}", 
            user.getId().getValue(),
            refreshToken.getId().value().toString(),
            tokenPair.refreshToken().getId().value().toString());
        
        return Result.success(RefreshTokenResponse.from(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tenantContext
        ));
    }
    
    /**
     * Creates a deterministic hash for token lookup
     * Uses SHA-256 instead of BCrypt because BCrypt is salted
     */
    private String createDeterministicHash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Extracts tenant context from user's bank assignments
     * Maintains the user's current tenant context during refresh (Requirement 2.5)
     */
    private Maybe<TenantContextDto> extractTenantContext(User user) {
        // Get user's bank assignments
        List<UserRole> bankAssignments = userRepository.userRolesFinder(user.getId());
        
        // Filter active assignments
        List<UserRole> activeAssignments = bankAssignments.stream()
            .filter(UserRole::isActive)
            .toList();
        
        if (activeAssignments.isEmpty()) {
            return Maybe.none();
        }
        
        // If user has a single bank, return that context
        // If multiple banks, we maintain the context from the JWT (which should be in the access token)
        // For now, we'll return the first active assignment as the default
        UserRole assignment = activeAssignments.get(0);
        
        Result<BankId> bankIdResult = BankId.fromString(assignment.getOrganizationId());
        if (bankIdResult.isFailure()) {
            return Maybe.none();
        }
        
        Result<TenantContext> tenantContextResult = TenantContext.create(
            bankIdResult.getValue().get(),
            assignment.getOrganizationId(), // Using organizationId as bankName for now
            assignment.getRoleName()
        );
        
        if (tenantContextResult.isFailure()) {
            return Maybe.none();
        }
        
        return Maybe.some(TenantContextDto.from(tenantContextResult.getValue().get()));
    }
}
