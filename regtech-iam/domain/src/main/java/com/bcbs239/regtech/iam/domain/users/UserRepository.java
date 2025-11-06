package com.bcbs239.regtech.iam.domain.users;






import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.List;
import java.util.function.Function;

/**
 * Domain repository interface for User aggregate operations.
 * Follows functional programming patterns with closures for persistence operations.
 */
public interface UserRepository {
    
    /**
     * Returns a function that loads a user by ID
     */
    Function<UserId, Result<User>> userLoader();
    
    /**
     * Returns a function that saves a user
     */
    Function<User, Result<UserId>> userSaver();
    
    /**
     * Returns a function that finds user roles by user ID
     */
    Function<UserId, List<UserRole>> userRolesFinder();
    
    /**
     * Returns a function that finds user organization roles
     */
    Function<UserOrgQuery, List<UserRole>> userOrgRolesFinder();
    
    /**
     * Returns a function that saves a user role
     */
    Function<UserRole, Result<String>> userRoleSaver();
    
    /**
     * Returns a function that finds user by email
     */
    Function<Email, Maybe<User>> emailLookup();
    
    /**
     * Returns a function that generates JWT tokens
     */
    Function<User, Result<JwtToken>> tokenGenerator(String secretKey);
    
    /**
     * Query object for user organization role lookups
     */
    record UserOrgQuery(UserId userId, String organizationId) {}
}


