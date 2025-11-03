package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.*;

import java.util.List;

/**
 * Authentication Result
 *
 * Contains the result of a successful authentication including user info,
 * JWT token, and bank selection requirements
 */
public record AuthenticationResult(
    UserId userId,
    Email email,
    JwtToken accessToken,
    boolean requiresBankSelection,
    List<BankAssignmentDto> availableBanks,
    Maybe<TenantContext> tenantContext, // Only if single bank
    String nextStep
) {

    /**
     * Factory method for single bank assignment (auto-selected)
     */
    public static AuthenticationResult withSingleBank(
            User user,
            JwtToken token,
            TenantContext tenantContext
    ) {
        return new AuthenticationResult(
            user.getId(),
            user.getEmail(),
            token,
            false,
            List.of(),
            Maybe.some(tenantContext),
            "DASHBOARD"
        );
    }

    /**
     * Factory method for multiple bank assignments (requires selection)
     */
    public static AuthenticationResult withMultipleBanks(
            User user,
            JwtToken token,
            List<BankAssignmentDto> availableBanks
    ) {
        return new AuthenticationResult(
            user.getId(),
            user.getEmail(),
            token,
            true,
            availableBanks,
            Maybe.none(),
            "SELECT_BANK"
        );
    }

    /**
     * Factory method for no bank assignments (requires configuration)
     */
    public static AuthenticationResult withNoBanks(
            User user,
            JwtToken token
    ) {
        return new AuthenticationResult(
            user.getId(),
            user.getEmail(),
            token,
            false,
            List.of(),
            Maybe.none(),
            "CONFIGURE_BANK"
        );
    }
}