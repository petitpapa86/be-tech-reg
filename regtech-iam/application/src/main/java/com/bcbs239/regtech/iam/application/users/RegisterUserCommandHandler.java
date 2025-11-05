package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ValidationUtils;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.iam.domain.users.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Component
public class RegisterUserCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(RegisterUserCommandHandler.class);

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
            ILogger.asyncStructuredLog("user registration started", Map.of(
                "email", command.email(),
                "bankId", command.bankId()
            ));

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
                logger.info("User registration completed successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_SUCCESS", Map.of(
                    "userId", response.userId().getValue(),
                    "correlationId", response.correlationId(),
                    "duration", duration
                )));
            } else {
                ErrorDetail error = result.getError().get();
                LoggingConfiguration.logError("user_registration", "REGISTRATION_FAILED", error.getMessage(), null, Map.of(
                    "errorCode", error.getCode(),
                    "duration", duration
                ));

                logger.error("User registration failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_FAILED", Map.of(
                    "errorCode", error.getCode(),
                    "errorMessage", error.getMessage(),
                    "duration", duration
                )));
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logError("user_registration", "UNEXPECTED_ERROR", e.getMessage(), e, Map.of(
                "email", command.email() != null ? command.email() : "null",
                "duration", duration
            ));

            logger.error("Unexpected error during user registration", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ERROR", Map.of(
                "email", command.email() != null ? command.email() : "null",
                "error", e.getMessage() != null ? e.getMessage() : "null",
                "duration", duration
            )), e);

            return Result.failure(ErrorDetail.of("UNEXPECTED_ERROR", "An unexpected error occurred"));
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
        String correlationId = "user-registration-" + UUID.randomUUID().toString();

        logger.info("Generated correlation ID for user registration",
                LoggingConfiguration.createStructuredLog("USER_REGISTRATION_CORRELATION_ID", Map.of(
                        "correlationId", correlationId,
                        "email", command.email() != null ? command.email() : "null"
                )));

        try {
            // Step 1: Validate and create email
            logger.debug("Validating email address", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_VALIDATION", Map.of(
                "correlationId", correlationId,
                "email", command.email() != null ? command.email() : "null"
            )));

            Result<Email> emailResult = Email.create(command.email());
            if (emailResult.isFailure()) {
                logger.warn("Email validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_INVALID", Map.of(
                    "correlationId", correlationId,
                    "email", command.email() != null ? command.email() : "null",
                    "error", emailResult.getError().get().getMessage()
                )));
                return Result.failure(emailResult.getError().get());
            }
            Email email = emailResult.getValue().get();

            logger.debug("Email validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_VALID", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            // Step 2: Check email uniqueness
            logger.debug("Checking email uniqueness", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_UNIQUENESS_CHECK", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            Maybe<User> existingUser = emailLookup.apply(email);
            if (existingUser.isPresent()) {
                logger.warn("Email already exists", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_EXISTS", Map.of(
                    "correlationId", correlationId,
                    "email", command.email() != null ? command.email() : "null",
                    "existingUserId", existingUser.getValue().getId().getValue()
                )));
                return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS",
                    "Email already exists"));
            }

            logger.debug("Email is unique", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_UNIQUE", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            // Step 3: Validate and create password
            logger.debug("Validating password", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_VALIDATION", Map.of(
                "correlationId", correlationId
            )));

            Result<Password> passwordResult = Password.create(command.password());
            if (passwordResult.isFailure()) {
                logger.warn("Password validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_INVALID", Map.of(
                    "correlationId", correlationId,
                    "error", passwordResult.getError().get().getMessage()
                )));
                return Result.failure(passwordResult.getError().get());
            }
            Password password = passwordResult.getValue().get();

            logger.debug("Password validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_VALID", Map.of(
                "correlationId", correlationId
            )));

            // Step 4: Validate names using Maybe to avoid nulls
            logger.debug("Validating user names", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_NAME_VALIDATION", Map.of(
                "correlationId", correlationId,
                "firstName", command.firstName() != null ? command.firstName() : "null",
                "lastName", command.lastName() != null ? command.lastName() : "null"
            )));

            Maybe<String> maybeFirst = ValidationUtils.validateName(command.firstName());
            if (maybeFirst.isEmpty()) {
                logger.warn("First name validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_FIRST_NAME_INVALID", Map.of(
                    "correlationId", correlationId,
                    "firstName", command.firstName() != null ? command.firstName() : "null"
                )));
                return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                    "First name is required and cannot be empty"));
            }
            String firstName = maybeFirst.getValue();

            Maybe<String> maybeLast = ValidationUtils.validateName(command.lastName());
            if (maybeLast.isEmpty()) {
                logger.warn("Last name validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_LAST_NAME_INVALID", Map.of(
                    "correlationId", correlationId,
                    "lastName", command.lastName() != null ? command.lastName() : "null"
                )));
                return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                    "Last name is required and cannot be empty"));
            }
            String lastName = maybeLast.getValue();

            logger.debug("Name validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_NAMES_VALID", Map.of(
                "correlationId", correlationId,
                "firstName", firstName,
                "lastName", lastName
            )));

            // Step 5: Create user aggregate with PENDING_PAYMENT status and bank assignment
            logger.debug("Creating user aggregate", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_CREATION", Map.of(
                "correlationId", correlationId,
                "email", command.email() != null ? command.email() : "null",
                "bankId", command.bankId() != null ? command.bankId() : "null",
                "firstName", firstName,
                "lastName", lastName
            )));

            User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId(),command.paymentMethodId());

            logger.debug("User aggregate created successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_CREATED", Map.of(
                "correlationId", correlationId,
                "userStatus", newUser.getStatus().toString(),
                "bankId", command.bankId() != null ? command.bankId() : "null"
            )));

            // Step 6: Save user
            logger.debug("Saving user to database", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVE_START", Map.of(
                "correlationId", correlationId,
                "email", command.email() != null ? command.email() : "null"
            )));

            Result<UserId> saveResult = userSaver.apply(newUser);
            if (saveResult.isFailure()) {
                logger.error("User save failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVE_FAILED", Map.of(
                    "correlationId", correlationId,
                    "email", command.email(),
                    "error", saveResult.getError().get().getMessage()
                )));
                return Result.failure(saveResult.getError().get());
            }

            UserId userId = saveResult.getValue().get();

            logger.info("User saved successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVED", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "email", command.email() != null ? command.email() : "null"
            )));

            // Step 7: Persist default ADMIN role for new users if a saver was provided
            if (userRoleSaver != null) {
                logger.debug("Assigning default admin role", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNMENT_START", Map.of(
                    "correlationId", correlationId,
                    "userId", userId.getValue()
                )));

                com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                    userId, Bcbs239Role.SYSTEM_ADMIN, "default-org"
                );
                Result<String> roleSaveResult = userRoleSaver.apply(adminRole);
                if (roleSaveResult.isFailure()) {
                    logger.error("Role assignment failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNMENT_FAILED", Map.of(
                        "correlationId", correlationId,
                        "userId", userId.getValue(),
                        "role", "ADMIN",
                        "error", roleSaveResult.getError().get().getMessage()
                    )));
                    return Result.failure(roleSaveResult.getError().get());
                }

                logger.debug("Admin role assigned successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNED", Map.of(
                    "correlationId", correlationId,
                    "userId", userId.getValue(),
                    "role", "ADMIN",
                    "organization", "default-org"
                )));
            }

            // Register user so its domain events are collected and published
            unitOfWork.registerEntity(newUser);

            // Step 8: Save changes (persist events to outbox)
            logger.debug("Saving changes via unit of work", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_UOW_START", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue()
            )));

            unitOfWork.saveChanges();

            logger.info("User registration completed via unit of work", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_UOW_SUCCESS", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "email", command.email(),
                "eventType", "UserRegisteredEvent"
            )));

            // Return success response
            RegisterUserResponse response = new RegisterUserResponse(userId, correlationId);
            logger.info("User registration workflow completed successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_WORKFLOW_SUCCESS", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "email", command.email() != null ? command.email() : "null",
                "bankId", command.bankId() != null ? command.bankId() : "null"
            )));

            return Result.success(response);

        } catch (Exception e) {
            logger.error("Unexpected error in user registration workflow", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_WORKFLOW_ERROR", Map.of(
                "correlationId", correlationId,
                "email", command.email() != null ? command.email() : "null",
                "error", e.getMessage() != null ? e.getMessage() : "null"
            )), e);
            throw e;
        }
    }


    /**
     * Data class for user registration information
     */
    public record UserRegistrationData(
        UserId userId,
        String paymentMethodId,
        String correlationId,
        String email,
        String name,
        String bankId,
        String phone,
        RegisterUserCommand.AddressInfo address
    ) {}
}

