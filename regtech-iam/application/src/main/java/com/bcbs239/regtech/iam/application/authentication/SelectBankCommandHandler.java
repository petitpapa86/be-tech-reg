package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.iam.domain.authentication.RefreshTokenId;
import com.bcbs239.regtech.iam.domain.authentication.TokenPair;
import com.bcbs239.regtech.iam.domain.users.TenantContext;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * SelectBankCommandHandler - Handles bank selection after login
 * 
 * Validates user access to selected bank and generates new token pair with tenant context
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 5.4, 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Component
@Slf4j
public class SelectBankCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final String jwtSecret;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    
    public SelectBankCommandHandler(
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
     * Handles the select bank command
     */
    @Transactional
    public Result<SelectBankResponse> handle(SelectBankCommand command) {
        // Log bank selection attempt
        log.info("BANK_SELECTION_ATTEMPT - userId: {}, bankId: {}", 
            command.userId().getValue(),
            command.bankId().getValue());
        
        // 1. Load user (Requirement 3.1)
        Maybe<User> userMaybe = userRepository.userLoader(command.userId());
        if (userMaybe.isEmpty()) {
            log.warn("BANK_SELECTION_FAILED_USER_NOT_FOUND - userId: {}", 
                command.userId().getValue());
            return Result.failure(ErrorDetail.of(
                "USER_NOT_FOUND",
                ErrorType.NOT_FOUND_ERROR,
                "User not found",
                "user.not_found"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 2. Validate user has access to the selected bank (Requirement 3.2, 3.4)
        List<UserRole> bankAssignments = userRepository.userRolesFinder(command.userId());
        
        Maybe<UserRole> selectedBankMaybe = bankAssignments.stream()
            .filter(UserRole::isActive)
            .filter(assignment -> assignment.getOrganizationId().equals(command.bankId().getValue()))
            .findFirst()
            .map(Maybe::some)
            .orElse(Maybe.none());
        
        if (selectedBankMaybe.isEmpty()) {
            log.warn("BANK_SELECTION_FORBIDDEN - userId: {}, bankId: {}", 
                command.userId().getValue(),
                command.bankId().getValue());
            return Result.failure(ErrorDetail.of(
                "BANK_ACCESS_DENIED",
                ErrorType.AUTHENTICATION_ERROR,
                "User does not have access to the selected bank",
                "select_bank.access_denied"
            ));
        }
        
        UserRole selectedBankAssignment = selectedBankMaybe.getValue();
        
        // 3. Create tenant context with selected bank (Requirement 3.3, 5.4, 10.2)
        Result<TenantContext> tenantContextResult = TenantContext.create(
            command.bankId(),
            selectedBankAssignment.getOrganizationId(), // Using organizationId as bankName for now
            selectedBankAssignment.getRoleName()
        );
        
        if (tenantContextResult.isFailure()) {
            log.error("BANK_SELECTION_FAILED_TENANT_CONTEXT - userId: {}, bankId: {}, error: {}", 
                command.userId().getValue(),
                command.bankId().getValue(),
                tenantContextResult.getError().get().getMessage());
            return Result.failure(tenantContextResult.getError().get());
        }
        
        TenantContext tenantContext = tenantContextResult.getValue().get();
        
        // 4. Generate new token pair with tenant context (Requirement 3.5, 5.4, 10.4, 10.5)
        Result<TokenPair> tokenPairResult = TokenPair.createWithTenantContext(
            user,
            tenantContext,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration,
            passwordHasher
        );
        
        if (tokenPairResult.isFailure()) {
            log.error("BANK_SELECTION_FAILED_TOKEN_GENERATION - userId: {}, bankId: {}, error: {}", 
                command.userId().getValue(),
                command.bankId().getValue(),
                tokenPairResult.getError().get().getMessage());
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 5. Save new refresh token (Requirement 6.1)
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            log.error("BANK_SELECTION_FAILED_TOKEN_SAVE - userId: {}, bankId: {}, error: {}", 
                command.userId().getValue(),
                command.bankId().getValue(),
                saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }
        
        // 6. Log successful bank selection
        log.info("BANK_SELECTED - userId: {}, bankId: {}, role: {}", 
            command.userId().getValue(),
            command.bankId().getValue(),
            selectedBankAssignment.getRoleName());
        
        return Result.success(SelectBankResponse.from(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tenantContext
        ));
    }
}
