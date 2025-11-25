package com.bcbs239.regtech.iam.domain.users;






import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;

import java.util.List;

/**
 * Domain repository interface for User aggregate operations.
 * Follows functional programming patterns with closures for persistence operations.
 */
public interface UserRepository {
    
    /**
     * Loads a user by ID
     */
    Maybe<User> userLoader(UserId userId);
    
    /**
     * Saves a user
     */
    Result<UserId> userSaver(User user);
    
    /**
     * Finds user roles by user ID
     */
    List<UserRole> userRolesFinder(UserId userId);
    
    /**
     * Finds user organization roles
     */
    List<UserRole> userOrgRolesFinder(UserOrgQuery query);
    
    /**
     * Saves a user role
     */
    Result<String> userRoleSaver(UserRole userRole);
    
    /**
     * Finds user by email
     */
    Maybe<User> emailLookup(Email email);
    
    /**
     * Generates JWT tokens for a user using provided secret
     */
    Result<JwtToken> tokenGenerator(User user, String secretKey);
    
    /**
     * Query object for user organization role lookups
     */
    record UserOrgQuery(UserId userId, String organizationId) {}
}


