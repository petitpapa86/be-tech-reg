package com.bcbs239.regtech.billing.application.shared;

import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;

/**
 * Data transfer object for user information extracted from saga context.
 * Contains the essential user data needed for billing account creation.
 */
public record UserData(
    UserId userId,
    String email,
    String name
) {
    
    /**
     * Factory method to create UserData
     */
    public static UserData of(UserId userId, String email, String name) {
        return new UserData(userId, email, name);
    }
}

