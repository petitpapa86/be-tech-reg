package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Command Handler: Invite new user
 * 
 * Creates user with PENDING_PAYMENT status + invitation token
 * Uses EXISTING User.create() factory method with proper value object validation
 */
@Service
@RequiredArgsConstructor
public class InviteUserHandler {
    
    private final UserRepository userRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @Value
    public static class Command {
        String bankId;
        String email;
        String firstName;
        String lastName;
        String invitedBy;
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

        // 3. Validate firstName
        if (command.firstName == null || command.firstName.trim().isEmpty()) {
            validationErrors.add(new FieldError("firstName", "First name is required", "validation.first_name_required"));
        }

        // 4. Validate lastName
        if (command.lastName == null || command.lastName.trim().isEmpty()) {
            validationErrors.add(new FieldError("lastName", "Last name is required", "validation.last_name_required"));
        }

        // 5. Validate invitedBy
        if (command.invitedBy == null || command.invitedBy.trim().isEmpty()) {
            validationErrors.add(new FieldError("invitedBy", "Invited by field is required", "validation.invited_by_required"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }
        
        // 6. Check if user already exists in this bank (business rule validation)
        if (userRepository.existsByEmailAndBankId(email, bankId.getAsLong())) {
            return Result.failure(
                ErrorDetail.of("USER_EXISTS", ErrorType.BUSINESS_RULE_ERROR,
                    "User with email " + command.email + " already exists in this bank",
                    "usermanagement.user.exists")
            );
        }
        
        // 7. Generate secure invitation token
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String invitationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // 8. Create temporary password hash (will be replaced when user accepts invitation)
        // For pending invitations, we use a placeholder hash since the real password will be set later
        Password tempPassword = Password.fromHash("PENDING_INVITATION_" + invitationToken.substring(0, 16));
        
        // 9. Create user using EXISTING factory method
        User user = User.create(email, tempPassword, command.firstName.trim(), command.lastName.trim());
        
        // 10. Assign to bank (adds BankAssignment)
        user.assignToBank(bankId.getValue(), "USER");
        
        // User status is already PENDING_PAYMENT by default from factory
        // Store invitation metadata (will be added to User entity or separate table)
        
        // 11. Save user
        Result<UserId> saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // TODO: Send invitation email with token
        
        return Result.success(user);
    }
}
