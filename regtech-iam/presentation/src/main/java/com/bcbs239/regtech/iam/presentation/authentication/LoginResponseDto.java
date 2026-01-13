package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.iam.application.authentication.LoginResponse;
import com.bcbs239.regtech.iam.application.authentication.TenantContextDto;

import java.time.Instant;
import java.util.List;

/**
 * Presentation DTO for login response
 * Converts from application layer LoginResponse to API response format
 */
public record LoginResponseDto(
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
    TenantContextDto tenantContext,
    String nextStep
) {
    /**
     * Converts from application layer LoginResponse to presentation DTO
     */
    public static LoginResponseDto from(LoginResponse response) {
        List<BankAssignmentDto> presentationBanks = response.availableBanks().stream()
            .map(BankAssignmentDto::from)
            .toList();
        
        List<BankAssignmentDto> presentationAssignments = response.bankAssignments().stream()
            .map(BankAssignmentDto::from)
            .toList();
        
        TenantContextDto tenantContext = response.tenantContext().isPresent()
            ? TenantContextDto.from(response.tenantContext().getValue())
            : null;
        
        return new LoginResponseDto(
            response.userId(),
            response.email(),
            response.username(),
            response.firstName(),
            response.lastName(),
            response.status(),
            response.accessToken(),
            response.refreshToken(),
            response.accessTokenExpiresAt(),
            response.refreshTokenExpiresAt(),
            response.requiresBankSelection(),
            presentationBanks,
            presentationAssignments,
            tenantContext,
            response.nextStep()
        );
    }
}
