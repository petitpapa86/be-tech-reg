package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Handler: Update user's role
 * 
 * Uses EXISTING UserRole and UserRepository with proper value object validation
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
        List<FieldError> validationErrors = new ArrayList<>();
        
        // 1. Validate and create UserId
        Result<UserId> userIdResult = UserId.create(command.userId);
        UserId userId = null;
        if (userIdResult.isFailure()) {
            validationErrors.add(new FieldError("userId", userIdResult.getError().get().getMessage(), userIdResult.getError().get().getMessageKey()));
        } else {
            userId = userIdResult.getValue().get();
        }

        // 2. Validate newRoleName
        if (command.newRoleName == null || command.newRoleName.trim().isEmpty()) {
            validationErrors.add(new FieldError("newRoleName", "Role name is required", "validation.role_name_required"));
        }

        // 3. Validate organizationId
        if (command.organizationId == null || command.organizationId.trim().isEmpty()) {
            validationErrors.add(new FieldError("organizationId", "Organization ID is required", "validation.organization_id_required"));
        }

        // 4. Validate modifiedBy
        if (command.modifiedBy == null || command.modifiedBy.trim().isEmpty()) {
            validationErrors.add(new FieldError("modifiedBy", "Modified by field is required", "validation.modified_by_required"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }
        
        // 5. Find user (business rule validation)
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure(
                ErrorDetail.of("USER_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "User not found", "usermanagement.user.not.found")
            );
        }
        
        // Validate role name exists
        // TODO: Query iam.roles to validate role exists
        
        // 6. Create new user role using EXISTING UserRole.create() with validation
        Result<UserRole> userRoleResult = UserRole.create(userId, command.newRoleName.trim(), command.organizationId.trim());
        if (userRoleResult.isFailure()) {
            return Result.failure(userRoleResult.getError().get());
        }
        
        UserRole userRole = userRoleResult.getValue().get();
        
        // 7. Save role
        Result<String> saveResult = userRepository.userRoleSaver(userRole);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(userRole);
    }
}
