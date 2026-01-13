package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.TenantContext;
import com.bcbs239.regtech.iam.domain.users.User;

import java.time.Instant;
import java.util.List;

/**
 * LoginResponse - Response DTO for login operation
 * 
 * Contains user information, tokens, and bank selection requirements
 * Requirements: 1.1, 1.2, 1.3, 10.1, 10.2
 */
public record LoginResponse(
    String userId,
    String email,
    String username,
    String firstName,
    String lastName,
    String status,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    boolean requiresBankSelection,
    List<BankAssignmentDto> availableBanks,
    List<BankAssignmentDto> bankAssignments,
    Maybe<TenantContext> tenantContext,
    String nextStep
) {
    /**
     * Creates a response for a user with a single bank assignment
     */
    public static LoginResponse withSingleBank(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        TenantContext tenantContext
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            mapBankAssignments(user),
            Maybe.some(tenantContext),
            "DASHBOARD"
        );
    }
    
    /**
     * Creates a response for a user with multiple bank assignments
     */
    public static LoginResponse withMultipleBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        List<BankAssignmentDto> availableBanks
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            true,
            availableBanks,
            mapBankAssignments(user),
            Maybe.none(),
            "SELECT_BANK"
        );
    }
    
    /**
     * Creates a response for a user with no bank assignments
     */
    public static LoginResponse withNoBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            mapBankAssignments(user),
            Maybe.none(),
            "CONFIGURE_BANK"
        );
    }

    private static List<BankAssignmentDto> mapBankAssignments(User user) {
        return user.getBankAssignments().stream()
            .map(ba -> BankAssignmentDto.from(ba.getBankId(), "Unknown Bank", ba.getRole()).getValue().get())
            .toList();
    }
}
