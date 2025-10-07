package com.bcbs239.regtech.iam.application.authenticate;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Command handler for user authentication with pure functional implementation
 */
@Component
public class AuthenticateUserCommandHandler {

    private final UserRepository userRepository;
    private final String jwtSecretKey;

    public AuthenticateUserCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        // TODO: Move to configuration
        this.jwtSecretKey = "mySecretKey123456789012345678901234567890123456789012345678901234567890";
    }

    /**
     * Handles the authenticate user command using pure functions
     */
    public Result<AuthenticationResult> handle(AuthenticationCommand command) {
        // Call the pure function with repository closures
        return authenticateUser(
            command,
            userRepository.emailLookup(),
            userRepository.tokenGenerator(jwtSecretKey)
        );
    }

    /**
     * Pure function for user authentication
     *
     * @param command The authentication command
     * @param userLookup Function to find user by email
     * @param tokenGenerator Function to generate JWT token
     * @return Result of authentication attempt
     */
    static Result<AuthenticationResult> authenticateUser(
            AuthenticationCommand command,
            Function<Email, Maybe<User>> userLookup,
            Function<User, Result<JwtToken>> tokenGenerator
    ) {
        // Validate command
        if (!command.isValid()) {
            return Result.failure(ErrorDetail.of("INVALID_COMMAND",
                "Email and password are required",
                "error.authentication.invalidCommand"));
        }

        // Create email value object
        Result<Email> emailResult = Email.create(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        Email email = emailResult.getValue().get();

        // Find user by email
        Maybe<User> userMaybe = userLookup.apply(email);
        if (userMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_CREDENTIALS",
                "Invalid email or password",
                "error.authentication.invalidCredentials"));
        }

        User user = userMaybe.getValue();

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("USER_NOT_ACTIVE",
                "User account is not active",
                "error.authentication.userNotActive"));
        }

        // Verify password
        if (!user.getPassword().matches(command.password())) {
            return Result.failure(ErrorDetail.of("INVALID_CREDENTIALS",
                "Invalid email or password",
                "error.authentication.invalidCredentials"));
        }

        // Generate JWT token
        Result<JwtToken> tokenResult = tokenGenerator.apply(user);
        if (tokenResult.isFailure()) {
            return Result.failure(tokenResult.getError().get());
        }

        JwtToken token = tokenResult.getValue().get();

        // Handle bank assignments
        List<User.BankAssignment> bankAssignments = user.getBankAssignments();

        if (bankAssignments.isEmpty()) {
            // No bank assignments - user needs to configure bank
            return Result.success(AuthenticationResult.withNoBanks(user, token));
        } else if (bankAssignments.size() == 1) {
            // Single bank assignment - auto-select
            User.BankAssignment assignment = bankAssignments.get(0);
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
            // Multiple bank assignments - user needs to select
            List<BankAssignmentDto> availableBanks = bankAssignments.stream()
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
}