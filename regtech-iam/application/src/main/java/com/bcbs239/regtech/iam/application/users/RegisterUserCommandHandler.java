package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.*;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Service
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;
    private final PasswordHasher passwordHasher;

    public RegisterUserCommandHandler(
            UserRepository userRepository,
            BaseUnitOfWork unitOfWork,
            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.unitOfWork = unitOfWork;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Handles the register user command with transactional outbox pattern
     */
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        List<FieldError> validationErrors = new ArrayList<>();

        // 1. Validate and create Email value object
        Result<Email> emailResult = Email.create(command.getEmail());
        Email email = null;
        if (emailResult.isFailure()) {
            validationErrors.add(new FieldError("email", emailResult.getError().get().getMessage(), emailResult.getError().get().getMessageKey()));
        } else {
            email = emailResult.getValue().get();
        }

        // 2. Validate and create BankId value object
        Result<BankId> bankIdResult = BankId.create(command.getBankId());
        BankId bankId = null;
        if (bankIdResult.isFailure()) {
            validationErrors.add(new FieldError("bankId", bankIdResult.getError().get().getMessage(), bankIdResult.getError().get().getMessageKey()));
        } else {
            bankId = bankIdResult.getValue().get();
        }

        // 3. Validate password strength
        Result<Void> passwordValidation = Password.validateStrength(command.getPassword());
        if (passwordValidation.isFailure()) {
            validationErrors.add(new FieldError("password", passwordValidation.getError().get().getMessage(), passwordValidation.getError().get().getMessageKey()));
        }

        // 4. Validate names
        Maybe<String> maybeFirst = ValidationUtils.validateName(command.getFirstName());
        if (maybeFirst.isEmpty()) {
            validationErrors.add(new FieldError("firstName", "First name is required and cannot be empty", "validation.first_name_required"));
        }

        Maybe<String> maybeLast = ValidationUtils.validateName(command.getLastName());
        if (maybeLast.isEmpty()) {
            validationErrors.add(new FieldError("lastName", "Last name is required and cannot be empty", "validation.last_name_required"));
        }

        // 5. Validate payment method ID
        if (command.getPaymentMethodId() == null || command.getPaymentMethodId().trim().isEmpty()) {
            validationErrors.add(new FieldError("paymentMethodId", "Payment method ID is required", "validation.payment_method_id_required"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }

        // 6. Check if user already exists (business rule validation)
        Maybe<User> existingUser = userRepository.emailLookup(email);
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS", ErrorType.VALIDATION_ERROR, "Email already exists", "user.email.already.exists"));
        }
        
        // 7. Hash the password using BCrypt
        String hashedPassword = passwordHasher.hash(command.getPassword());
        Password password = Password.fromHash(hashedPassword);

        String firstName = maybeFirst.getValue();
        String lastName = maybeLast.getValue();

        // 8. Create user with validated value objects
        User newUser = User.createWithBank(email, password, firstName, lastName, bankId.getValue(), command.getPaymentMethodId());

        // 9. Save user
        Result<UserId> saveResult = userRepository.userSaver(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        UserId userId = saveResult.getValue().get();

        // 10. Create user role with validation
        Result<UserRole> roleResult = UserRole.create(userId, "SYSTEM_ADMIN", "default-org");
        if (roleResult.isFailure()) {
            return Result.failure(roleResult.getError().get());
        }

        UserRole adminRole = roleResult.getValue().get();
        Result<String> roleSaveResult = userRepository.userRoleSaver(adminRole);
        if (roleSaveResult.isFailure()) {
            return Result.failure(roleSaveResult.getError().get());
        }

        unitOfWork.registerEntity(newUser);
        unitOfWork.saveChanges();

        return Result.success(new RegisterUserResponse(userId, CorrelationContext.correlationId()));
    }
}

