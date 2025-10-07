package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisterUserCommandHandler Tests")
class RegisterUserCommandHandlerTest {

    @Nested
    @DisplayName("Successful Registration")
    class SuccessfulRegistration {

        @Test
        @DisplayName("Should successfully register user with valid data")
        void shouldSuccessfullyRegisterUserWithValidData() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john.doe@example.com",
                "ValidPass123!",
                "John",
                "Doe"
            );

            UserId expectedUserId = UserId.generate();
            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(expectedUserId);

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isPresent();
            RegisterUserResponse response = result.getValue().get();
            assertThat(response.userId()).isEqualTo(expectedUserId);
            assertThat(response.correlationId()).isNotNull();
            assertThat(isValidUUID(response.correlationId())).isTrue();
        }

        @Test
        @DisplayName("Should generate unique correlation ID for each request")
        void shouldGenerateUniqueCorrelationIdForEachRequest() {
            // Given
            RegisterUserCommand command1 = new RegisterUserCommand(
                "user1@example.com", "ValidPass123!", "User", "One"
            );
            RegisterUserCommand command2 = new RegisterUserCommand(
                "user2@example.com", "ValidPass123!", "User", "Two"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result1 = RegisterUserCommandHandler.registerUser(command1, emailLookup, userSaver);
            Result<RegisterUserResponse> result2 = RegisterUserCommandHandler.registerUser(command2, emailLookup, userSaver);

            // Then
            assertThat(result1.getValue().get().correlationId())
                .isNotEqualTo(result2.getValue().get().correlationId());
        }
    }

    @Nested
    @DisplayName("Email Validation Failures")
    class EmailValidationFailures {

        @Test
        @DisplayName("Should fail when email is null")
        void shouldFailWhenEmailIsNull() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                null, "ValidPass123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();
            ErrorDetail error = result.getError().get();
            assertThat(error.getCode()).isEqualTo("EMAIL_REQUIRED");
            assertThat(error.getMessage()).isEqualTo("Email is required");
        }

        @Test
        @DisplayName("Should fail when email is empty")
        void shouldFailWhenEmailIsEmpty() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "", "ValidPass123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("EMAIL_REQUIRED");
        }

        @Test
        @DisplayName("Should fail when email format is invalid")
        void shouldFailWhenEmailFormatIsInvalid() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "invalid-email", "ValidPass123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("EMAIL_INVALID");
        }
    }

    @Nested
    @DisplayName("Email Uniqueness Check")
    class EmailUniquenessCheck {

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldFailWhenEmailAlreadyExists() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "existing@example.com", "ValidPass123!", "John", "Doe"
            );

            User existingUser = User.create(
                Email.create("existing@example.com").getValue().get(),
                Password.create("ValidPass123!").getValue().get(),
                "Existing", "User"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.some(existingUser);
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();
            ErrorDetail error = result.getError().get();
            assertThat(error.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
            assertThat(error.getMessage()).isEqualTo("Email already exists");
        }
    }

    @Nested
    @DisplayName("Password Validation Failures")
    class PasswordValidationFailures {

        @Test
        @DisplayName("Should fail when password is null")
        void shouldFailWhenPasswordIsNull() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", null, "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_TOO_SHORT");
        }

        @Test
        @DisplayName("Should fail when password is too short")
        void shouldFailWhenPasswordIsTooShort() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "short", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_TOO_SHORT");
        }

        @Test
        @DisplayName("Should fail when password lacks uppercase")
        void shouldFailWhenPasswordLacksUppercase() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "validpass123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_MISSING_UPPERCASE");
        }

        @Test
        @DisplayName("Should fail when password lacks lowercase")
        void shouldFailWhenPasswordLacksLowercase() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "VALIDPASS123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_MISSING_LOWERCASE");
        }

        @Test
        @DisplayName("Should fail when password lacks digit")
        void shouldFailWhenPasswordLacksDigit() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPassword!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_MISSING_DIGIT");
        }

        @Test
        @DisplayName("Should fail when password lacks special character")
        void shouldFailWhenPasswordLacksSpecialCharacter() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("PASSWORD_MISSING_SPECIAL");
        }
    }

    @Nested
    @DisplayName("Name Validation Failures")
    class NameValidationFailures {

        @Test
        @DisplayName("Should fail when first name is null")
        void shouldFailWhenFirstNameIsNull() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", null, "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("INVALID_FIRST_NAME");
        }

        @Test
        @DisplayName("Should fail when first name is empty")
        void shouldFailWhenFirstNameIsEmpty() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", "", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("INVALID_FIRST_NAME");
        }

        @Test
        @DisplayName("Should fail when last name is null")
        void shouldFailWhenLastNameIsNull() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", "John", null
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("INVALID_LAST_NAME");
        }

        @Test
        @DisplayName("Should fail when last name is empty")
        void shouldFailWhenLastNameIsEmpty() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", "John", ""
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("INVALID_LAST_NAME");
        }
    }

    @Nested
    @DisplayName("User Save Failures")
    class UserSaveFailures {

        @Test
        @DisplayName("Should fail when user save operation fails")
        void shouldFailWhenUserSaveOperationFails() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user ->
                Result.failure(ErrorDetail.of("USER_SAVE_FAILED", "Database error", "error.save.failed"));

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();
            ErrorDetail error = result.getError().get();
            assertThat(error.getCode()).isEqualTo("USER_SAVE_FAILED");
            assertThat(error.getMessage()).isEqualTo("Database error");
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Should handle multiple validation failures in sequence")
        void shouldHandleMultipleValidationFailuresInSequence() {
            // Given - command with multiple issues
            RegisterUserCommand command = new RegisterUserCommand(
                "invalid-email", "weak", "John", "Doe"
            );

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> Result.success(UserId.generate());

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then - should fail at first validation (email format)
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getCode()).isEqualTo("EMAIL_INVALID");
        }

        @Test
        @DisplayName("Should trim whitespace from names")
        void shouldTrimWhitespaceFromNames() {
            // Given
            RegisterUserCommand command = new RegisterUserCommand(
                "john@example.com", "ValidPass123!", "  John  ", "  Doe  "
            );

            UserId expectedUserId = UserId.generate();

            Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
            Function<User, Result<UserId>> userSaver = user -> {
                // Verify names are trimmed
                assertThat(user.getFirstName()).isEqualTo("John");
                assertThat(user.getLastName()).isEqualTo("Doe");
                return Result.success(expectedUserId);
            };

            // When
            Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(command, emailLookup, userSaver);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}