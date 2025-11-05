package com.bcbs239.regtech.iam.domain.users;



import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import io.jsonwebtoken.*;

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
            String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiresAt))
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();

            return Result.success(new JwtToken(token, expiresAt));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("JWT_GENERATION_FAILED",
                "Failed to generate JWT token: " + e.getMessage()));
        }
    }

    /**
     * Validates a JWT token and extracts claims
     */
    public static Result<JwtClaims> validate(String token, String secretKey) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();

            return Result.success(new JwtClaims(claims));
        } catch (ExpiredJwtException e) {
            return Result.failure(ErrorDetail.of("JWT_EXPIRED",
                "JWT token has expired"));
        } catch (MalformedJwtException e) {
            return Result.failure(ErrorDetail.of("JWT_MALFORMED",
                "JWT token is malformed"));
        } catch (SignatureException e) {
            return Result.failure(ErrorDetail.of("JWT_INVALID_SIGNATURE",
                "JWT token has invalid signature"));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("JWT_VALIDATION_FAILED",
                "JWT token validation failed: " + e.getMessage()));
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
    }
}

