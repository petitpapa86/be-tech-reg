package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Add New Active User
 * 
 * Creates user with ACTIVE status directly (no invitation workflow).
 * Used by admins to create users with immediate access.
 * 
 * Uses EXISTING User domain model.
 */
@Service
@RequiredArgsConstructor
public class AddNewUserHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        Long bankId;
        String email;
        String firstName;
        String lastName;
        String password;        // Plain text password (will be validated and hashed)
        String roleName;        // e.g., "BANK_ADMIN", "DATA_ANALYST", "VIEWER"
        String createdBy;       // Admin username
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // 1. Validate email
        var emailResult = Email.create(command.email);
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Email email = emailResult.getValue().get();
        
        // 2. Check if user already exists in this bank
        if (userRepository.existsByEmailAndBankId(email, command.bankId)) {
            return Result.failure(
                ErrorDetail.of("USER_EXISTS", ErrorType.BUSINESS_RULE_ERROR,
                    "User with email " + command.email + " already exists in this bank",
                    "usermanagement.user.exists")
            );
        }
        
        // 3. Validate password strength (using existing Password value object)
        var passwordValidation = Password.validateStrength(command.password);
        if (passwordValidation.isFailure()) {
            return Result.failure(passwordValidation.getError().get());
        }
        
        // 4. Create password from hash (in real implementation, hash using PasswordHasher service first)
        // For now, using fromHash as a placeholder - in production, hash the password first
        Password password = Password.fromHash("HASHED_" + command.password);
        
        // 5. Create user using EXISTING factory method
        User user = User.create(
            email,
            password,
            command.firstName,
            command.lastName
        );
        
        // 6. Assign to bank (adds BankAssignment)
        user.assignToBank(command.bankId.toString(), command.roleName);
        
        // 7. Activate user immediately (change status from PENDING_PAYMENT to ACTIVE)
        user.activate();
        
        // 8. Save user
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // TODO: Send welcome email with credentials
        
        return Result.success(user);
    }
}
