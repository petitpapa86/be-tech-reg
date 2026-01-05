package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Suspend user account
 * 
 * Uses EXISTING User.suspend() domain method
 */
@Service
@RequiredArgsConstructor
public class SuspendUserHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
        String suspendedBy;
    }
    
    @Transactional
    public Result<User> handle(Command command) {
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
        
        // Use EXISTING domain method
        user.suspend();
        
        // Save
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(user);
    }
}
