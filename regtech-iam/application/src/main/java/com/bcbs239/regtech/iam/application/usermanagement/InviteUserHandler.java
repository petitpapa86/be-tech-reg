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
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Command Handler: Invite new user
 * 
 * Creates user with PENDING_PAYMENT status + invitation token
 * Uses EXISTING User.create() factory method
 */
@Service
@RequiredArgsConstructor
public class InviteUserHandler {
    
    private final UserRepository userRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @Value
    public static class Command {
        Long bankId;
        String email;
        String firstName;
        String lastName;
        String invitedBy;
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // Validate email
        var emailResult = Email.create(command.email);
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Email email = emailResult.getValue().get();
        
        // Check if user already exists in this bank
        if (userRepository.existsByEmailAndBankId(email, command.bankId)) {
            return Result.failure(
                ErrorDetail.of("USER_EXISTS", ErrorType.BUSINESS_RULE_ERROR,
                    "User with email " + command.email + " already exists in this bank",
                    "usermanagement.user.exists")
            );
        }
        
        // Generate secure invitation token
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String invitationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Create temporary password hash (will be replaced when user accepts invitation)
        // For pending invitations, we use a placeholder hash since the real password will be set later
        Password tempPassword = Password.fromHash("PENDING_INVITATION_" + invitationToken.substring(0, 16));
        
        // Create user using EXISTING factory method
        User user = User.create(
            email,
            tempPassword,
            command.firstName,
            command.lastName
        );
        
        // Assign to bank (adds BankAssignment)
        user.assignToBank(command.bankId.toString(), "USER");
        
        // User status is already PENDING_PAYMENT by default from factory
        // Store invitation metadata (will be added to User entity or separate table)
        
        // Save user
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // TODO: Send invitation email with token
        
        return Result.success(user);
    }
}
