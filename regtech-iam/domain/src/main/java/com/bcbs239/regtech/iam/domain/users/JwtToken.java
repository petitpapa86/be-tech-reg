package com.bcbs239.regtech.iam.domain.users;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT Token Value Object
 *
 * Represents a JSON Web Token with claims and expiration
 */
public record JwtToken(String value, Instant expiresAt) {

    /**
     * Generates a JWT token for a user with their bank assignments
     */
    public static Result<JwtToken> generate(
            User user,
            String secretKey,
            Duration expiration
    ) {
        try {
            // Build claims map with user information and bank assignments
            Map<String, Object> claims = Map.of(
                "userId", user.getId().getValue().toString(),
                "email", user.getEmail().getValue(),
                "banks", user.getBankAssignments().stream()
                    .map(assignment -> Map.of(
                        "bankId", assignment.getBankId(),
                        "role", assignment.getRole()
                    ))
                    .toList()
            );

            Instant expiresAt = Instant.now().plus(expiration);
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

            return Result.success(new JwtToken(token, expiresAt));
        } catch (Exception e) {
            // Log the actual error for debugging (security issue details)
            org.slf4j.LoggerFactory.getLogger(JwtToken.class).error("JWT generation failed: {}", e.getMessage(), e);
            // Return generic error message to client (don't expose security details)
            return Result.failure(ErrorDetail.of("JWT_GENERATION_FAILED", ErrorType.AUTHENTICATION_ERROR, "Failed to generate authentication token", "jwt.generation.failed"));
        }
    }
    
    /**
     * Generates a JWT token for a user with specific tenant context
     * Used for bank selection flow
     */
    public static Result<JwtToken> generateWithTenantContext(
            User user,
            TenantContext tenantContext,
            String secretKey,
            Duration expiration
    ) {
        try {
            // Build claims map with user information and selected tenant context
            Map<String, Object> claims = Map.of(
                "userId", user.getId().getValue().toString(),
                "email", user.getEmail().getValue(),
                "bankId", tenantContext.bankId().getValue(),
                "bankName", tenantContext.bankName(),
                "role", tenantContext.roleName()
            );

            Instant expiresAt = Instant.now().plus(expiration);
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

            return Result.success(new JwtToken(token, expiresAt));
        } catch (Exception e) {
            // Log the actual error for debugging (security issue details)
            org.slf4j.LoggerFactory.getLogger(JwtToken.class).error("JWT generation failed: {}", e.getMessage(), e);
            // Return generic error message to client (don't expose security details)
            return Result.failure(ErrorDetail.of("JWT_GENERATION_FAILED", ErrorType.AUTHENTICATION_ERROR, "Failed to generate authentication token", "jwt.generation.failed"));
        }
    }

    /**
     * Validates a JWT token and extracts claims
     */
    public static Result<JwtClaims> validate(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return Result.success(new JwtClaims(claims));
        } catch (ExpiredJwtException e) {
            return Result.failure(ErrorDetail.of("JWT_EXPIRED", ErrorType.AUTHENTICATION_ERROR, "JWT token has expired", "jwt.expired"));
        } catch (MalformedJwtException e) {
            return Result.failure(ErrorDetail.of("JWT_MALFORMED", ErrorType.AUTHENTICATION_ERROR, "JWT token is malformed", "jwt.malformed"));
        } catch (SignatureException e) {
            return Result.failure(ErrorDetail.of("JWT_INVALID_SIGNATURE", ErrorType.AUTHENTICATION_ERROR, "JWT token has invalid signature", "jwt.invalid_signature"));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("JWT_VALIDATION_FAILED", ErrorType.AUTHENTICATION_ERROR, "JWT token validation failed: " + e.getMessage(), "jwt.validation.failed"));
        }
    }

    /**
     * JWT Claims wrapper
     */
    public static class JwtClaims {
        private final Claims claims;

        public JwtClaims(Claims claims) {
            this.claims = claims;
        }

        public String getUserId() {
            return claims.get("userId", String.class);
        }

        public String getEmail() {
            return claims.get("email", String.class);
        }

        @SuppressWarnings("unchecked")
        public List<Map<String, String>> getBanks() {
            return claims.get("banks", List.class);
        }

        public Date getIssuedAt() {
            return claims.getIssuedAt();
        }

        public Date getExpiration() {
            return claims.getExpiration();
        }
        
        /**
         * Get the underlying Claims object for accessing additional claims.
         * Used by SecurityFilter to extract bankId and role.
         */
        public Claims getClaims() {
            return claims;
        }
    }
}



