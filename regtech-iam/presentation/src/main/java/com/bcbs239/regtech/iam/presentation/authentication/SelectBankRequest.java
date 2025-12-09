package com.bcbs239.regtech.iam.presentation.authentication;

/**
 * Request DTO for bank selection
 */
public record SelectBankRequest(
    String userId,
    String bankId,
    String refreshToken
) {}
