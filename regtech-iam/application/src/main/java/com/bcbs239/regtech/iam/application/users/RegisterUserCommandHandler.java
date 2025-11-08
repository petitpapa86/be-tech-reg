package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ValidationUtils;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Service
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
        
        // Inline registerUser logic to simplify flow and remove indirection
        String correlationId = "user-registration-" + UUID.randomUUID();

        try {
            // Step 1: Validate and create email
            Result<Email> emailResult = Email.create(command.email());
            if (emailResult.isFailure()) {
                return Result.failure(emailResult.getError().get());
            }
            Email email = emailResult.getValue().get();

            // Step 2: Check email uniqueness
            Maybe<User> existingUser = userRepository.emailLookup(email);
            if (existingUser.isPresent()) {
                return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS", ErrorType.VALIDATION_ERROR, "Email already exists", "user.email.already.exists"));
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
                return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME", ErrorType.VALIDATION_ERROR, "First name is required and cannot be empty", "user.invalid.first.name"));
            }
            String firstName = maybeFirst.getValue();

            Maybe<String> maybeLast = ValidationUtils.validateName(command.lastName());
            if (maybeLast.isEmpty()) {
                return Result.failure(ErrorDetail.of("INVALID_LAST_NAME", ErrorType.VALIDATION_ERROR, "Last name is required and cannot be empty", "user.invalid.last.name"));
            }
            String lastName = maybeLast.getValue();

            // Step 5: Create user aggregate with PENDING_PAYMENT status and bank assignment
            User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId(), command.paymentMethodId());

            // Step 6: Save user
            Result<UserId> saveResult = userRepository.userSaver(newUser);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().get());
            }

            UserId userId = saveResult.getValue().get();

            // Step 7: Persist default ADMIN role for new users
            com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                userId, Bcbs239Role.SYSTEM_ADMIN, "default-org"
            );
            Result<String> roleSaveResult = userRepository.userRoleSaver(adminRole);
            if (roleSaveResult.isFailure()) {
                return Result.failure(roleSaveResult.getError().get());
            }

            // Register user so its domain events are collected and published
            unitOfWork.registerEntity(newUser);

            // Step 8: Save changes (persist events to outbox)
            unitOfWork.saveChanges();

            // Return success response
            RegisterUserResponse response = new RegisterUserResponse(userId, correlationId);
            return Result.success(response);
        } catch (Exception e) {
            throw new RuntimeException("User registration failed", e);
        }

        
    }


}

