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
 * Command Handler: Update user's role
 * 
 * Uses EXISTING UserRole and UserRepository
 */
@Service
@RequiredArgsConstructor
public class UpdateUserRoleHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
        String newRoleName; // e.g., "BANK_ADMIN", "DATA_ANALYST"
        String organizationId;
        String modifiedBy;
    }
    
    @Transactional
    public Result<UserRole> handle(Command command) {
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
        
        // Validate role name exists
        // TODO: Query iam.roles to validate role exists
        
        // Create new user role using EXISTING UserRole.create()
        UserRole userRole = UserRole.create(userId, command.newRoleName, command.organizationId);
        
        // Save role
        var saveResult = userRepository.userRoleSaver(userRole);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(userRole);
    }
}
