package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;

/**
 * User role entity that links users to their roles and permissions.
 * Now uses database-driven role names instead of deprecated enum.
 */
@Getter
public class UserRole {

    private String id;
    private String userId;
    private String roleName; // Database-driven role name
    private String organizationId; // For multi-tenant support
    private boolean active;

    // Private constructor to enforce factory method usage
    private UserRole() {}

    /**
     * Creates a new UserRole with validation
     */
    public static Result<UserRole> create(UserId userId, String roleName, String organizationId) {
        if (userId == null) {
            return Result.failure(ErrorDetail.of(
                "USER_ID_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "User ID is required for role assignment", 
                "validation.user_id_required"
            ));
        }

        if (roleName == null || roleName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "ROLE_NAME_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Role name cannot be null or empty", 
                "validation.role_name_required"
            ));
        }

        if (organizationId == null || organizationId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "ORGANIZATION_ID_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Organization ID is required for role assignment", 
                "validation.organization_id_required"
            ));
        }

        UserRole userRole = new UserRole();
        userRole.userId = userId.getValue();
        userRole.roleName = roleName.trim();
        userRole.organizationId = organizationId.trim();
        userRole.active = true;
        
        return Result.success(userRole);
    }

    /**
     * Factory method for persistence layer reconstruction
     */
    public static UserRole createFromPersistence(String id, String userId, String roleName, 
                                                String organizationId, boolean active) {
        UserRole userRole = new UserRole();
        userRole.id = id;
        userRole.userId = userId;
        userRole.roleName = roleName;
        userRole.organizationId = organizationId;
        userRole.active = active;
        return userRole;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}


