package com.bcbs239.regtech.ingestion.application.service;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token validation and bank ID extraction.
 * This is a simplified implementation for the current development phase.
 * In production, this would integrate with a proper JWT library and validation.
 */
@Service
public class JwtTokenService {
    
    /**
     * Extract bank ID from JWT token.
     * Currently simplified for development - in production this would:
     * 1. Validate JWT signature
     * 2. Check expiration
     * 3. Extract claims
     * 4. Return bank ID from claims
     */
    public Result<BankId> extractBankId(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validationError("authToken", authToken, "Auth token cannot be null or empty"));
        }
        
        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;
        
        if (token.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validationError("authToken", authToken, "Auth token cannot be empty after Bearer prefix"));
        }
        
        // For development/testing purposes, extract bank ID from a simple format
        // In production, this would parse a real JWT token
        try {
            // Expect format like "bank_12345" or just "12345"
            String bankIdValue = token.startsWith("bank_") ? token : "bank_" + token;
            
            // Validate the bank ID format
            if (bankIdValue.length() > 20) {
                return Result.failure(ErrorDetail.validationError("bankId", bankIdValue, "Bank ID cannot exceed 20 characters"));
            }
            
            return Result.success(BankId.of(bankIdValue));
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.validationError("authToken", authToken, "Invalid token format: " + e.getMessage()));
        }
    }
    
    /**
     * Validate JWT token.
     * Currently simplified - in production would validate signature, expiration, etc.
     */
    public Result<Void> validateToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validationError("authToken", authToken, "Auth token cannot be null or empty"));
        }
        
        // For development, just check basic format
        String token = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;
        
        if (token.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validationError("authToken", authToken, "Auth token cannot be empty"));
        }
        
        // In production, this would validate JWT signature, expiration, etc.
        return Result.success(null);
    }
}