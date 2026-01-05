package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.core.application.Command;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.domain.users.Password;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for user registration with Stripe payment information
 */
@Getter
@Setter
public final class RegisterUserCommand extends Command {

    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String bankId;
    private final String paymentMethodId;
    private final String phone;
    private final AddressInfo address;

    public RegisterUserCommand(String commandId,
                               String email,
                               String password,
                               String firstName,
                               String lastName,
                               String bankId,
                               String paymentMethodId,
                               String phone,
                               AddressInfo address) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Address information for Stripe integration
     */
    public static record AddressInfo(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {}

    /**
     * Creates a RegisterUserCommand with validation using value objects
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

        // Validate email using Email value object
        Result<Email> emailResult = Email.create(email);
        if (emailResult.isFailure()) {
            fieldErrors.add(new FieldError("email", emailResult.getError().get().getMessage(), emailResult.getError().get().getMessageKey()));
        }

        // Validate password using Password value object
        Result<Void> passwordResult = Password.validateStrength(password);
        if (passwordResult.isFailure()) {
            fieldErrors.add(new FieldError("password", passwordResult.getError().get().getMessage(), passwordResult.getError().get().getMessageKey()));
        }

        // Validate firstName
        if (firstName == null || firstName.trim().isEmpty()) {
            fieldErrors.add(new FieldError("firstName", "First name is required", "error.firstName.required"));
        }

        // Validate lastName
        if (lastName == null || lastName.trim().isEmpty()) {
            fieldErrors.add(new FieldError("lastName", "Last name is required", "error.lastName.required"));
        }

        // Validate bankId using BankId value object
        Result<BankId> bankIdResult = BankId.create(bankId);
        if (bankIdResult.isFailure()) {
            fieldErrors.add(new FieldError("bankId", bankIdResult.getError().get().getMessage(), bankIdResult.getError().get().getMessageKey()));
        }

        // Validate paymentMethodId
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            fieldErrors.add(new FieldError("paymentMethodId", "Payment method ID is required", "error.paymentMethodId.required"));
        }

        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }

        return Result.success(new RegisterUserCommand(
            java.util.UUID.randomUUID().toString(),
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

