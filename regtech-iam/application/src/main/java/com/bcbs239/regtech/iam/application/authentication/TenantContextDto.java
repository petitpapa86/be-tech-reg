package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.iam.domain.users.TenantContext;

/**
 * DTO for tenant context information
 * Requirements: 10.2, 10.5
 */
public record TenantContextDto(
    String bankId,
    String bankName,
    String role
) {
    /**
     * Factory method to create DTO from domain object
     */
    public static TenantContextDto from(TenantContext context) {
        return new TenantContextDto(
            context.bankId().getValue(),
            context.bankName(),
            context.roleName()
        );
    }
}
