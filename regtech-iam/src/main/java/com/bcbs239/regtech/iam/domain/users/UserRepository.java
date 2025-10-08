package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.application.authenticate.OAuth2UserInfo;

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
     * Returns a function that finds user by email
     */
    Function<Email, Maybe<User>> emailLookup();
    
    /**
     * Returns a function that saves OAuth user
     */
    Function<OAuth2UserInfo, Result<User>> saveOAuthUser();
    
    /**
     * Returns a function that generates JWT tokens
     */
    Function<User, Result<JwtToken>> tokenGenerator(String secretKey);
    
    /**
     * Query object for user organization role lookups
     */
    record UserOrgQuery(UserId userId, String organizationId) {}
}