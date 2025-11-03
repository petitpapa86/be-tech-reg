package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for user registration with Stripe payment information
 */
public record RegisterUserCommand(
    String email,
    String password,
    String firstName,
    String lastName,
    String bankId,
    String paymentMethodId,
    String phone,
    AddressInfo address
) {

    /**
     * Address information for Stripe integration
     */
    public record AddressInfo(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {}
    /**
     * Creates a RegisterUserCommand with validation
     *
     * @param email User's email
     * @param password User's password
     * @param firstName User's first name
     * @param lastName User's last name
     * @param bankId User's bank ID for aggregation queries
     * @param paymentMethodId Stripe payment method ID
     * @param phone User's phone number (optional)
     * @param address User's address information (optional)
     * @return Result containing the command if valid, or validation errors
     */
    public static Result<RegisterUserCommand> create(
            String email,
            String password,
            String firstName,
            String lastName,
            String bankId,
            String paymentMethodId,
            String phone,
            AddressInfo address) {

        List<FieldError> fieldErrors = new ArrayList<>();

        // Validate email
        if (email == null || email.trim().isEmpty()) {
            fieldErrors.add(new FieldError("email", "REQUIRED", "Email is required", "error.email.required"));
        }

        // Validate password
        if (password == null || password.trim().isEmpty()) {
            fieldErrors.add(new FieldError("password", "REQUIRED", "Password is required", "error.password.required"));
        }

        // Validate firstName
        if (firstName == null || firstName.trim().isEmpty()) {
            fieldErrors.add(new FieldError("firstName", "REQUIRED", "First name is required", "error.firstName.required"));
        }

        // Validate lastName
        if (lastName == null || lastName.trim().isEmpty()) {
            fieldErrors.add(new FieldError("lastName", "REQUIRED", "Last name is required", "error.lastName.required"));
        }

        // Validate bankId
        if (bankId == null || bankId.trim().isEmpty()) {
            fieldErrors.add(new FieldError("bankId", "REQUIRED", "Bank ID is required", "error.bankId.required"));
        }

        // Validate paymentMethodId
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            fieldErrors.add(new FieldError("paymentMethodId", "REQUIRED", "Payment method ID is required", "error.paymentMethodId.required"));
        }

        if (!fieldErrors.isEmpty()) {
            return Result.failure(new ErrorDetail("VALIDATION_ERROR", "Invalid input data", "error.validation", null, fieldErrors));
        }

        return Result.success(new RegisterUserCommand(
            email.trim(),
            password.trim(),
            firstName.trim(),
            lastName.trim(),
            bankId.trim(),
            paymentMethodId.trim(),
            phone != null ? phone.trim() : null,
            address
        ));
    }
}