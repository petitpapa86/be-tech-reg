package com.bcbs239.regtech.iam.application.authenticate;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Command handler for OAuth2 authentication
 */
@Component
public class OAuth2AuthenticateUserCommandHandler {

    private final UserRepository userRepository;
    private final OAuth2ProviderService oAuth2ProviderService;
    private final String jwtSecretKey;

    public OAuth2AuthenticateUserCommandHandler(
            UserRepository userRepository,
            OAuth2ProviderService oAuth2ProviderService) {
        this.userRepository = userRepository;
        this.oAuth2ProviderService = oAuth2ProviderService;
        // TODO: Move to configuration
        this.jwtSecretKey = "mySecretKey123456789012345678901234567890123456789012345678901234567890";
    }

    /**
     * Handles OAuth2 authentication
     */
    public Result<AuthenticationResult> handle(OAuth2AuthenticationCommand command) {
        return authenticateWithOAuth2(
            command,
            oAuth2ProviderService::verifyAndGetUserInfo,
            userRepository.emailLookup(),
            userRepository.saveOAuthUser(),
            userRepository.tokenGenerator(jwtSecretKey)
        );
    }

    /**
     * Pure function for OAuth2 authentication
     */
    static Result<AuthenticationResult> authenticateWithOAuth2(
            OAuth2AuthenticationCommand command,
            Function<OAuth2AuthenticationCommand, Result<OAuth2UserInfo>> tokenVerifier,
            Function<Email, Maybe<User>> userLookup,
            Function<OAuth2UserInfo, Result<User>> userSaver,
            Function<User, Result<JwtToken>> tokenGenerator
    ) {
        // Validate command
        if (!command.isValid()) {
            return Result.failure(ErrorDetail.of("INVALID_OAUTH_COMMAND",
                "Invalid OAuth2 provider or token",
                "error.oauth.invalidCommand"));
        }

        // Verify OAuth token with provider
        Result<OAuth2UserInfo> verificationResult = tokenVerifier.apply(command);
        if (verificationResult.isFailure()) {
            return Result.failure(verificationResult.getError().get());
        }

        OAuth2UserInfo userInfo = verificationResult.getValue().get();

        // Check if user already exists
        Maybe<User> existingUser = userLookup.apply(userInfo.email());
        User user;

        if (existingUser.isPresent()) {
            // Existing user - update OAuth ID if needed
            user = existingUser.getValue();
            updateOAuthId(user, command.provider(), userInfo.externalId());
        } else {
            // New user - create via OAuth
            Result<User> createResult = userSaver.apply(userInfo);
            if (createResult.isFailure()) {
                return Result.failure(createResult.getError().get());
            }
            user = createResult.getValue().get();
        }

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("USER_NOT_ACTIVE",
                "User account is not active",
                "error.authentication.userNotActive"));
        }

        // Generate JWT token
        Result<JwtToken> tokenResult = tokenGenerator.apply(user);
        if (tokenResult.isFailure()) {
            return Result.failure(tokenResult.getError().get());
        }

        JwtToken token = tokenResult.getValue().get();

        // Handle bank assignments (same logic as regular authentication)
        var bankAssignments = user.getBankAssignments();

        if (bankAssignments.isEmpty()) {
            return Result.success(AuthenticationResult.withNoBanks(user, token));
        } else if (bankAssignments.size() == 1) {
            var assignment = bankAssignments.get(0);
            Result<BankId> bankIdResult = BankId.fromString(assignment.getBankId());
            if (bankIdResult.isFailure()) {
                return Result.failure(bankIdResult.getError().get());
            }

            Result<TenantContext> tenantContextResult = TenantContext.create(
                bankIdResult.getValue().get(),
                "Bank Name", // TODO: Get from bank service
                Bcbs239Role.valueOf(assignment.getRole().toUpperCase())
            );
            if (tenantContextResult.isFailure()) {
                return Result.failure(tenantContextResult.getError().get());
            }

            return Result.success(AuthenticationResult.withSingleBank(user, token, tenantContextResult.getValue().get()));
        } else {
            var availableBanks = bankAssignments.stream()
                .map(assignment -> BankAssignmentDto.from(
                    assignment.getBankId(),
                    "Bank Name", // TODO: Get from bank service
                    assignment.getRole()
                ))
                .filter(result -> result.isSuccess())
                .map(result -> result.getValue().get())
                .toList();
            return Result.success(AuthenticationResult.withMultipleBanks(user, token, availableBanks));
        }
    }

    private static void updateOAuthId(User user, String provider, String externalId) {
        if ("google".equals(provider)) {
            user.setGoogleId(externalId);
        } else if ("facebook".equals(provider)) {
            user.setFacebookId(externalId);
        }
    }

    /**
     * OAuth2 Provider Service interface
     */
    @FunctionalInterface
    public interface OAuth2ProviderService {
        Result<OAuth2UserInfo> verifyAndGetUserInfo(OAuth2AuthenticationCommand command);
    }
}