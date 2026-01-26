package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Handler: Add New Active User
 * 
 * Creates user with ACTIVE status directly (no invitation workflow).
 * Used by admins to create users with immediate access.
 * 
 * Uses EXISTING User domain model with proper value object validation.
 */
@Service
@RequiredArgsConstructor
public class AddNewUserHandler {
    
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    
    @Value
    public static class Command {
        String bankId;
        String email;
        String firstName;
        String lastName;
        String password;        // Plain text password (will be validated and hashed)
        String roleName;        // e.g., "BANK_ADMIN", "DATA_ANALYST", "VIEWER"
        String createdBy;       // Admin username
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        List<FieldError> validationErrors = new ArrayList<>();
        
        // 1. Validate and create Email value object
        Result<Email> emailResult = Email.create(command.email);
        Email email = null;
        if (emailResult.isFailure()) {
            validationErrors.add(new FieldError("email", emailResult.getError().get().getMessage(), emailResult.getError().get().getMessageKey()));
        } else {
            email = emailResult.getValue().get();
        }

        // 2. Validate and create BankId value object with numeric validation
        Result<BankId> bankIdResult = BankId.createNumeric(command.bankId);
        BankId bankId = null;
        if (bankIdResult.isFailure()) {
            validationErrors.add(new FieldError("bankId", bankIdResult.getError().get().getMessage(), bankIdResult.getError().get().getMessageKey()));
        } else {
            bankId = bankIdResult.getValue().get();
        }

        // 3. Validate password strength
        Result<Void> passwordValidation = Password.validateStrength(command.password);
        if (passwordValidation.isFailure()) {
            validationErrors.add(new FieldError("password", passwordValidation.getError().get().getMessage(), passwordValidation.getError().get().getMessageKey()));
        }

        // 4. Validate firstName
        if (command.firstName == null || command.firstName.trim().isEmpty()) {
            validationErrors.add(new FieldError("firstName", "First name is required", "validation.first_name_required"));
        }

        // 5. Validate lastName
        if (command.lastName == null || command.lastName.trim().isEmpty()) {
            validationErrors.add(new FieldError("lastName", "Last name is required", "validation.last_name_required"));
        }

        // 6. Validate roleName
        if (command.roleName == null || command.roleName.trim().isEmpty()) {
            validationErrors.add(new FieldError("roleName", "Role name is required", "validation.role_name_required"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }
        
        // 7. Check if user already exists in this bank (business rule validation)
        if (userRepository.existsByEmailAndBankId(email, bankId.getAsLong())) {
            return Result.failure(
                ErrorDetail.of("USER_EXISTS", ErrorType.BUSINESS_RULE_ERROR,
                    "User with email " + command.email + " already exists in this bank",
                    "usermanagement.user.exists")
            );
        }
        
        // 8. Hash the password and create Password value object
        String hashedPassword = passwordHasher.hash(command.password);
        Password password = Password.fromHash(hashedPassword);
        
        // 9. Create user using EXISTING factory method
        User user = User.create(email, password, command.firstName.trim(), command.lastName.trim());
        // 10. Assign to bank (adds BankAssignment)
        user.assignToBank(bankId.getValue(), command.roleName.trim());
        // 11. User is ACTIVE by default (no invitation, no payment required)
        
        // 12. Save user
        Result<UserId> saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // TODO: Send welcome email with credentials
        
        return Result.success(user);
    }
}
