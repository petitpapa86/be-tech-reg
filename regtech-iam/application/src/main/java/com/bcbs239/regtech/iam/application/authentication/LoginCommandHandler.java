package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.iam.domain.authentication.RefreshTokenId;
import com.bcbs239.regtech.iam.domain.authentication.TokenPair;
import com.bcbs239.regtech.iam.domain.users.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * LoginCommandHandler - Handles user login with email and password
 * 
 * Authenticates users, generates token pairs, and handles bank selection logic
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 5.4, 9.1, 9.2, 10.1, 10.2
 */
@Component
@Slf4j
public class LoginCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final String jwtSecret;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    
    public LoginCommandHandler(
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
     * Handles the login command
     */
    @Transactional
    public Result<LoginResponse> handle(LoginCommand command) {
        // Log login attempt (Requirement 9.1)
        log.info("LOGIN_ATTEMPT - email: {}, ipAddress: {}", 
            command.email(), 
            command.ipAddress().orElse("unknown"));
        
        // 1. Find user by email (Requirement 1.1)
        Result<Email> emailResult = Email.create(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Maybe<User> userMaybe = userRepository.emailLookup(emailResult.getValue().get());
        
        if (userMaybe.isEmpty()) {
            // Log failed attempt (Requirement 9.2)
            log.warn("LOGIN_FAILED_USER_NOT_FOUND - email: {}", command.email());
            // Return generic error to prevent user enumeration (Requirement 8.1)
            return Result.failure(ErrorDetail.of(
                "INVALID_CREDENTIALS",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid email or password",
                "login.invalid_credentials"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 2. Verify password (Requirement 1.1, 5.3, 12.3)
        if (!passwordHasher.verify(command.password(), user.getPassword().getHashedValue())) {
            // Log failed attempt (Requirement 9.2)
            log.warn("LOGIN_FAILED_INVALID_PASSWORD - userId: {}, email: {}", 
                user.getId().getValue(), 
                command.email());
            // Return generic error to prevent user enumeration (Requirement 8.1)
            return Result.failure(ErrorDetail.of(
                "INVALID_CREDENTIALS",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid email or password",
                "login.invalid_credentials"
            ));
        }
        
        // 3. Check if user is active (Requirement 1.5)
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("LOGIN_FAILED_USER_INACTIVE - userId: {}, status: {}", 
                user.getId().getValue(),
                user.getStatus());
            return Result.failure(ErrorDetail.of(
                "ACCOUNT_DISABLED",
                ErrorType.AUTHENTICATION_ERROR,
                "Account is disabled",
                "login.account_disabled"
            ));
        }
        
        // 4. Generate token pair (Requirement 5.1, 5.2, 5.3, 5.4)
        Result<TokenPair> tokenPairResult = TokenPair.create(
            user,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration,
            passwordHasher
        );
        
        if (tokenPairResult.isFailure()) {
            log.error("LOGIN_FAILED_TOKEN_GENERATION - userId: {}, error: {}", 
                user.getId().getValue(),
                tokenPairResult.getError().get().getMessage());
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 5. Save refresh token (Requirement 6.1)
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            log.error("LOGIN_FAILED_TOKEN_SAVE - userId: {}, error: {}", 
                user.getId().getValue(),
                saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }
        
        // 6. Determine bank selection requirements (Requirement 1.2, 1.3, 10.1, 10.2)
        List<UserRole> bankAssignments = userRepository.userRolesFinder(user.getId());
        
        // Filter only active bank assignments
        List<UserRole> activeBankAssignments = bankAssignments.stream()
            .filter(UserRole::isActive)
            .toList();
        
        LoginResponse response;
        if (activeBankAssignments.isEmpty()) {
            // No bank assignments (Requirement 10.3)
            response = LoginResponse.withNoBanks(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken()
            );
            log.info("LOGIN_SUCCESS_NO_BANKS - userId: {}, email: {}, ipAddress: {}", 
                user.getId().getValue(),
                user.getEmail().getValue(),
                command.ipAddress().orElse("unknown"));
        } else if (activeBankAssignments.size() == 1) {
            // Single bank - auto-select (Requirement 1.2)
            UserRole assignment = activeBankAssignments.get(0);
            Result<BankId> bankIdResult = BankId.fromString(assignment.getOrganizationId());
            if (bankIdResult.isFailure()) {
                return Result.failure(bankIdResult.getError().get());
            }
            
            Result<TenantContext> tenantContextResult = TenantContext.create(
                bankIdResult.getValue().get(),
                assignment.getOrganizationId(), // Using organizationId as bankName for now
                assignment.getRoleName()
            );
            if (tenantContextResult.isFailure()) {
                return Result.failure(tenantContextResult.getError().get());
            }
            
            response = LoginResponse.withSingleBank(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tenantContextResult.getValue().get()
            );
            log.info("LOGIN_SUCCESS_SINGLE_BANK - userId: {}, email: {}, bankId: {}, role: {}, ipAddress: {}", 
                user.getId().getValue(),
                user.getEmail().getValue(),
                assignment.getOrganizationId(),
                assignment.getRoleName(),
                command.ipAddress().orElse("unknown"));
        } else {
            // Multiple banks - require selection (Requirement 1.3)
            List<BankAssignmentDto> availableBanks = activeBankAssignments.stream()
                .map(assignment -> {
                    Result<BankAssignmentDto> dtoResult = BankAssignmentDto.from(
                        assignment.getOrganizationId(),
                        assignment.getOrganizationId(), // Using organizationId as bankName for now
                        assignment.getRoleName()
                    );
                    return dtoResult.getValue().orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            response = LoginResponse.withMultipleBanks(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                availableBanks
            );
            log.info("LOGIN_SUCCESS_MULTIPLE_BANKS - userId: {}, email: {}, bankCount: {}, ipAddress: {}", 
                user.getId().getValue(),
                user.getEmail().getValue(),
                availableBanks.size(),
                command.ipAddress().orElse("unknown"));
        }
        
        // 7. Record authentication (update user's last login)
        user.recordAuthentication();
        userRepository.userSaver(user);
        
        return Result.success(response);
    }
}
