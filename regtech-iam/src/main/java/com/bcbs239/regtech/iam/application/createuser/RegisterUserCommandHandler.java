package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with pure functional implementation
 */
@Component
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;

    public RegisterUserCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles the register user command using pure functions
     */
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // Call the pure function with repository closures
        return registerUser(command, userRepository.emailLookup(), userRepository.userSaver());
    }

    /**
     * Pure function for user registration
     *
     * @param command The registration command
     * @param emailLookup Function to check if email exists
     * @param userRepository Function to save the user
     * @return Result of registration attempt
     */
    private static Result<RegisterUserResponse> registerUser(
            RegisterUserCommand command,
            Function<Email, Maybe<User>> emailLookup,
            Function<User, Result<UserId>> userRepository
    ) {
        // Generate correlation ID for saga tracking
        String correlationId = UUID.randomUUID().toString();

        // Validate and create email
        Result<Email> emailResult = Email.create(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        Email email = emailResult.getValue().get();

        // Check email uniqueness
        Maybe<User> existingUser = emailLookup.apply(email);
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS",
                "Email already exists", "error.email.exists"));
        }

        // Validate and create password
        Result<Password> passwordResult = Password.create(command.password());
        if (passwordResult.isFailure()) {
            return Result.failure(passwordResult.getError().get());
        }
        Password password = passwordResult.getValue().get();

        // Validate names
        String firstName = validateName(command.firstName(), "firstName");
        if (firstName == null) {
            return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                "First name is required and cannot be empty", "error.firstName.required"));
        }

        String lastName = validateName(command.lastName(), "lastName");
        if (lastName == null) {
            return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                "Last name is required and cannot be empty", "error.lastName.required"));
        }

        // Create user aggregate
        User newUser = User.create(email, password, firstName, lastName);

        // Save user
        Result<UserId> saveResult = userRepository.apply(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Return success response
        RegisterUserResponse response = new RegisterUserResponse(saveResult.getValue().get(), correlationId);
        return Result.success(response);
    }

    private static String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return name.trim();
    }
}