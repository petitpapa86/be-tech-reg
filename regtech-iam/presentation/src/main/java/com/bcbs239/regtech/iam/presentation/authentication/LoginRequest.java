package com.bcbs239.regtech.iam.presentation.authentication;

/**
 * Request DTO for user login
 */
public record LoginRequest(
    String email,
    String password
) {}
