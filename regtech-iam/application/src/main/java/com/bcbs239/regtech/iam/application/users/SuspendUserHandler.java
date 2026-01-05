package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Suspend user account
 * 
 * Uses EXISTING User.suspend() domain method with proper value object validation
 */
@Service
@RequiredArgsConstructor
public class SuspendUserHandler {
    
    private final UserRepository userRepository;

    public record Command(String userId, String suspendedBy) {
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // Validate and create UserId
        Result<UserId> userIdResult = UserId.create(command.userId);
        if (userIdResult.isFailure()) {
            return Result.failure(userIdResult.getError().get());
        }
        UserId userId = userIdResult.getValue().get();
        
        // Find user
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure(
                ErrorDetail.of("USER_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "User not found", "usermanagement.user.not.found")
            );
        }
        
        User user = userMaybe.getValue();

        user.suspend();
        
        Result<UserId> saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(user);
    }
}
