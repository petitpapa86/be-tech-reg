package com.bcbs239.regtech.iam.presentation.usermanagement;

import jakarta.validation.constraints.*;

/**
 * Request DTO for adding a new active user (no invitation)
 */
public record AddUserRequest(
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    String password,
    
    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "VIEWER|DATA_ANALYST|AUDITOR|RISK_MANAGER|COMPLIANCE_OFFICER|BANK_ADMIN|HOLDING_COMPANY_USER|SYSTEM_ADMIN",
        message = "Invalid role"
    )
    String role
) {}
