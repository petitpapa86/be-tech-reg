package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Revoke pending user invitation
 * 
 * Only works for users with PENDING_PAYMENT status
 */
@Service
@RequiredArgsConstructor
public class RevokeInvitationHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
    }
    
    @Transactional
    public Result<Void> handle(Command command) {
        // Parse user ID
        UserId userId;
        try {
            userId = UserId.fromString(command.userId);
        } catch (IllegalArgumentException e) {
            return Result.failure(
                ErrorDetail.of("INVALID_USER_ID", ErrorType.VALIDATION_ERROR,
                    "Invalid user ID format", "usermanagement.invalid.user.id")
            );
        }
        
        // Find user
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure(
                ErrorDetail.of("USER_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "User not found", "usermanagement.user.not.found")
            );
        }
        
        User user = userMaybe.getValue();
        
        // Business rule: Can only revoke pending invitations
        if (user.getStatus() != UserStatus.PENDING_PAYMENT) {
            return Result.failure(
                ErrorDetail.of("INVALID_STATUS", ErrorType.BUSINESS_RULE_ERROR,
                    "Can only revoke pending invitations", "usermanagement.invalid.status")
            );
        }
        
        // Delete user
        userRepository.deleteUser(userId);
        
        return Result.success(null);
    }
}
