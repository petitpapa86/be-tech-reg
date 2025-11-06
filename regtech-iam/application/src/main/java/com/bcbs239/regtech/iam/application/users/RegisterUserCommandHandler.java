package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ValidationUtils;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Component
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;

    public RegisterUserCommandHandler(
            UserRepository userRepository,
            BaseUnitOfWork unitOfWork) {
        this.userRepository = userRepository;
        this.unitOfWork = unitOfWork;
    }

    /**
     * Handles the register user command with transactional outbox pattern
     */
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        long startTime = System.currentTimeMillis();

        try {
            // Call the pure function with repository closures and unit of work
            Result<RegisterUserResponse> result = registerUser(
                command,
                userRepository.emailLookup(),
                userRepository.userSaver(),
                unitOfWork,
                userRepository.userRoleSaver()
            );

            long duration = System.currentTimeMillis() - startTime;
            if (result.isSuccess()) {
                RegisterUserResponse response = result.getValue().get();
            } else {
                ErrorDetail error = result.getError().get();
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new RuntimeException("User registration failed", e);
        }
    }

    /**
     * Pure function for user registration with transactional outbox pattern
     *
     * @param command The registration command
     * @param emailLookup Function to check if email exists
     * @param userSaver Function to save the user
     * @param unitOfWork Unit of work for transactional operations
     * @return Result of registration attempt
     */
    @Transactional
    static Result<RegisterUserResponse> registerUser(
        RegisterUserCommand command,
        Function<Email, Maybe<User>> emailLookup,
        Function<User, Result<UserId>> userSaver,
        BaseUnitOfWork unitOfWork
    ) {
    // Fallback to overload that doesn't persist roles (keeps existing tests working)
    return registerUser(command, emailLookup, userSaver, unitOfWork, null);
    }

    @Transactional
    static Result<RegisterUserResponse> registerUser(
        RegisterUserCommand command,
        Function<Email, Maybe<User>> emailLookup,
        Function<User, Result<UserId>> userSaver,
        BaseUnitOfWork unitOfWork,
        java.util.function.Function<com.bcbs239.regtech.iam.domain.users.UserRole, Result<String>> userRoleSaver
    ) {
        // Generate correlation ID for saga tracking with user data embedded
        String correlationId = "user-registration-" + UUID.randomUUID();

        try {
            // Step 1: Validate and create email
            Result<Email> emailResult = Email.create(command.email());
            if (emailResult.isFailure()) {
                return Result.failure(emailResult.getError().get());
            }
            Email email = emailResult.getValue().get();

            // Step 2: Check email uniqueness
            Maybe<User> existingUser = emailLookup.apply(email);
            if (existingUser.isPresent()) {
                return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS",
                    "Email already exists"));
            }

            // Step 3: Validate and create password
            Result<Password> passwordResult = Password.create(command.password());
            if (passwordResult.isFailure()) {
                return Result.failure(passwordResult.getError().get());
            }
            Password password = passwordResult.getValue().get();

            // Step 4: Validate names using Maybe to avoid nulls
            Maybe<String> maybeFirst = ValidationUtils.validateName(command.firstName());
            if (maybeFirst.isEmpty()) {
                return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                    "First name is required and cannot be empty"));
            }
            String firstName = maybeFirst.getValue();

            Maybe<String> maybeLast = ValidationUtils.validateName(command.lastName());
            if (maybeLast.isEmpty()) {
                return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                    "Last name is required and cannot be empty"));
            }
            String lastName = maybeLast.getValue();

            // Step 5: Create user aggregate with PENDING_PAYMENT status and bank assignment
            User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId(),command.paymentMethodId());

            // Step 6: Save user
            Result<UserId> saveResult = userSaver.apply(newUser);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().get());
            }

            UserId userId = saveResult.getValue().get();

            // Step 7: Persist default ADMIN role for new users if a saver was provided
            if (userRoleSaver != null) {
                com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                    userId, Bcbs239Role.SYSTEM_ADMIN, "default-org"
                );
                Result<String> roleSaveResult = userRoleSaver.apply(adminRole);
                if (roleSaveResult.isFailure()) {
                    return Result.failure(roleSaveResult.getError().get());
                }
            }

            // Register user so its domain events are collected and published
            unitOfWork.registerEntity(newUser);

            // Step 8: Save changes (persist events to outbox)
            unitOfWork.saveChanges();

            // Return success response
            RegisterUserResponse response = new RegisterUserResponse(userId, correlationId);
            return Result.success(response);

        } catch (Exception e) {
            throw e;
        }
    }

}

