package com.bcbs239.regtech.iam.presentation.authentication;

/**
 * Presentation DTO representing a bank assignment for a user
 * This is a simple DTO for API responses
 */
public record BankAssignmentDto(
    String bankId,
    String bankName,
    String role
) {
    /**
     * Converts from application layer BankAssignmentDto to presentation DTO
     */
    public static BankAssignmentDto from(com.bcbs239.regtech.iam.application.authentication.BankAssignmentDto appDto) {
        return new BankAssignmentDto(
            appDto.bankId().value(),
            appDto.bankName(),
            appDto.roleName()
        );
    }
}
