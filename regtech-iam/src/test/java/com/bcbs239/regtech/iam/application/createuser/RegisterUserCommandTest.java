package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production-ready unit tests for RegisterUserCommand validation.
 * Tests cover success cases, individual field validation, multiple errors,
 * and input sanitization using Result-based validation.
 */
@DisplayName("RegisterUserCommand Validation Tests")
class RegisterUserCommandTest {

    @Nested
    @DisplayName("Successful Command Creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("Should create command successfully with valid inputs")
        void shouldCreateCommandSuccessfullyWithValidInputs() {
            // Given
            String email = "john.doe@example.com";
            String password = "securePassword123";
            String firstName = "John";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isPresent();

            RegisterUserCommand command = result.getValue().get();
            assertThat(command.email()).isEqualTo(email);
            assertThat(command.password()).isEqualTo(password);
            assertThat(command.firstName()).isEqualTo(firstName);
            assertThat(command.lastName()).isEqualTo(lastName);
        }

        @Test
        @DisplayName("Should trim whitespace from inputs")
        void shouldTrimWhitespaceFromInputs() {
            // Given
            String email = "  john.doe@example.com  ";
            String password = "  securePassword123  ";
            String firstName = "  John  ";
            String lastName = "  Doe  ";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RegisterUserCommand command = result.getValue().get();
            assertThat(command.email()).isEqualTo("john.doe@example.com");
            assertThat(command.password()).isEqualTo("securePassword123");
            assertThat(command.firstName()).isEqualTo("John");
            assertThat(command.lastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should handle minimum valid inputs")
        void shouldHandleMinimumValidInputs() {
            // Given - Single character names are valid
            String email = "a@b.co";
            String password = "pass";
            String firstName = "A";
            String lastName = "B";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RegisterUserCommand command = result.getValue().get();
            assertThat(command.email()).isEqualTo(email);
            assertThat(command.password()).isEqualTo(password);
            assertThat(command.firstName()).isEqualTo(firstName);
            assertThat(command.lastName()).isEqualTo(lastName);
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail when email is null, empty, or whitespace only")
        void shouldFailWhenEmailIsNullOrEmpty(String invalidEmail) {
            // Given
            String password = "validPassword123";
            String firstName = "John";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(invalidEmail, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();

            ErrorDetail error = result.getError().get();
            assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.getMessage()).isEqualTo("Invalid input data");
            assertThat(error.getMessageKey()).isEqualTo("error.validation");
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(1);

            FieldError emailError = fieldErrors.get(0);
            assertThat(emailError.getField()).isEqualTo("email");
            assertThat(emailError.getCode()).isEqualTo("REQUIRED");
            assertThat(emailError.getMessage()).isEqualTo("Email is required");
            assertThat(emailError.getMessageKey()).isEqualTo("error.email.required");
        }
    }

    @Nested
    @DisplayName("Password Validation")
    class PasswordValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail when password is null, empty, or whitespace only")
        void shouldFailWhenPasswordIsNullOrEmpty(String invalidPassword) {
            // Given
            String email = "john.doe@example.com";
            String firstName = "John";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, invalidPassword, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(1);

            FieldError passwordError = fieldErrors.get(0);
            assertThat(passwordError.getField()).isEqualTo("password");
            assertThat(passwordError.getCode()).isEqualTo("REQUIRED");
            assertThat(passwordError.getMessage()).isEqualTo("Password is required");
            assertThat(passwordError.getMessageKey()).isEqualTo("error.password.required");
        }
    }

    @Nested
    @DisplayName("First Name Validation")
    class FirstNameValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail when firstName is null, empty, or whitespace only")
        void shouldFailWhenFirstNameIsNullOrEmpty(String invalidFirstName) {
            // Given
            String email = "john.doe@example.com";
            String password = "validPassword123";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, invalidFirstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(1);

            FieldError firstNameError = fieldErrors.get(0);
            assertThat(firstNameError.getField()).isEqualTo("firstName");
            assertThat(firstNameError.getCode()).isEqualTo("REQUIRED");
            assertThat(firstNameError.getMessage()).isEqualTo("First name is required");
            assertThat(firstNameError.getMessageKey()).isEqualTo("error.firstName.required");
        }
    }

    @Nested
    @DisplayName("Last Name Validation")
    class LastNameValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail when lastName is null, empty, or whitespace only")
        void shouldFailWhenLastNameIsNullOrEmpty(String invalidLastName) {
            // Given
            String email = "john.doe@example.com";
            String password = "validPassword123";
            String firstName = "John";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, invalidLastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(1);

            FieldError lastNameError = fieldErrors.get(0);
            assertThat(lastNameError.getField()).isEqualTo("lastName");
            assertThat(lastNameError.getCode()).isEqualTo("REQUIRED");
            assertThat(lastNameError.getMessage()).isEqualTo("Last name is required");
            assertThat(lastNameError.getMessageKey()).isEqualTo("error.lastName.required");
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors")
    class MultipleValidationErrors {

        @Test
        @DisplayName("Should collect all validation errors when multiple fields are invalid")
        void shouldCollectAllValidationErrorsWhenMultipleFieldsAreInvalid() {
            // Given - All fields are null
            String email = null;
            String password = null;
            String firstName = null;
            String lastName = null;

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(4);

            // Verify all field errors are present
            assertThat(fieldErrors).extracting(FieldError::getField)
                .containsExactlyInAnyOrder("email", "password", "firstName", "lastName");

            assertThat(fieldErrors).allMatch(fieldError ->
                fieldError.getCode().equals("REQUIRED") &&
                fieldError.getMessage().contains("is required") &&
                fieldError.getMessageKey().startsWith("error.") &&
                fieldError.getMessageKey().endsWith(".required")
            );
        }

        @Test
        @DisplayName("Should collect validation errors for subset of invalid fields")
        void shouldCollectValidationErrorsForSubsetOfInvalidFields() {
            // Given - Only email and password are invalid
            String email = "";
            String password = "   ";
            String firstName = "John";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(2);

            assertThat(fieldErrors).extracting(FieldError::getField)
                .containsExactlyInAnyOrder("email", "password");
        }

        @Test
        @DisplayName("Should handle mixed whitespace-only and null inputs")
        void shouldHandleMixedWhitespaceOnlyAndNullInputs() {
            // Given - Mix of null, empty, and whitespace-only inputs
            String email = null;
            String password = "";
            String firstName = "   ";
            String lastName = "\t\n";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();
            assertThat(error.hasFieldErrors()).isTrue();

            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(4);

            assertThat(fieldErrors).extracting(FieldError::getField)
                .containsExactlyInAnyOrder("email", "password", "firstName", "lastName");
        }
    }

    @Nested
    @DisplayName("Error Response Structure")
    class ErrorResponseStructure {

        @Test
        @DisplayName("Should return consistent error structure for validation failures")
        void shouldReturnConsistentErrorStructureForValidationFailures() {
            // Given
            String email = "john.doe@example.com";
            String password = ""; // Invalid
            String firstName = "John";
            String lastName = "Doe";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            ErrorDetail error = result.getError().get();

            // Verify error structure
            assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.getMessage()).isEqualTo("Invalid input data");
            assertThat(error.getMessageKey()).isEqualTo("error.validation");
            assertThat(error.getDetails()).isNull(); // No additional details for validation errors
            assertThat(error.hasFieldErrors()).isTrue();

            // Verify field error structure
            List<FieldError> fieldErrors = error.getFieldErrors();
            assertThat(fieldErrors).hasSize(1);

            FieldError fieldError = fieldErrors.get(0);
            assertThat(fieldError.getField()).isEqualTo("password");
            assertThat(fieldError.getCode()).isEqualTo("REQUIRED");
            assertThat(fieldError.getMessage()).isEqualTo("Password is required");
            assertThat(fieldError.getMessageKey()).isEqualTo("error.password.required");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle extremely long inputs")
        void shouldHandleExtremelyLongInputs() {
            // Given - Very long inputs (should still be valid if not empty)
            String longString = "a".repeat(1000);
            String email = "test@example.com";
            String password = longString;
            String firstName = longString;
            String lastName = longString;

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RegisterUserCommand command = result.getValue().get();
            assertThat(command.password()).isEqualTo(longString);
            assertThat(command.firstName()).isEqualTo(longString);
            assertThat(command.lastName()).isEqualTo(longString);
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Given - Names with special characters (should be valid if not empty)
            String email = "test@example.com";
            String password = "password123";
            String firstName = "José-María";
            String lastName = "O'Connor";

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RegisterUserCommand command = result.getValue().get();
            assertThat(command.firstName()).isEqualTo(firstName);
            assertThat(command.lastName()).isEqualTo(lastName);
        }

        @Test
        @DisplayName("Should handle unicode characters in inputs")
        void shouldHandleUnicodeCharactersInInputs() {
            // Given - Unicode characters
            String email = "tëst@example.com";
            String password = "pássword123";
            String firstName = "José";
            String lastName = "Михаил"; // Cyrillic

            // When
            Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, "BANK001", "pm_test123", null, null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            RegisterUserCommand command = result.getValue().get();
            assertThat(command.email()).isEqualTo(email);
            assertThat(command.password()).isEqualTo(password);
            assertThat(command.firstName()).isEqualTo(firstName);
            assertThat(command.lastName()).isEqualTo(lastName);
        }
    }
}