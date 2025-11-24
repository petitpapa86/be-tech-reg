package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.domain.security.Authentication;
import com.bcbs239.regtech.core.domain.security.SecurityContext;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.AnonymousAuthentication;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.DefaultSecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SimpleAuthentication;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Security filter that handles JWT authentication and sets up security context.
 * Uses Java 21 Scoped Values for thread-safe context management.
 * 
 * Public paths are loaded from configuration (iam.security.public-paths).
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final String jwtSecret;
    private final List<String> publicPaths;

    /**
     * Constructor that accepts IAMProperties to load JWT secret and public paths from configuration.
     * 
     * @param iamProperties Configuration properties containing JWT secret and public paths
     * @throws IllegalArgumentException if public paths configuration is invalid
     * Requirements: 11.1, 11.5
     */
    public SecurityFilter(IAMProperties iamProperties) {
        this.jwtSecret = iamProperties.getSecurity().getJwt().getSecret();
        this.publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Validate JWT secret
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured");
        }
        
        // Validate public paths configuration (Requirements: 11.4, 11.5)
        validatePublicPaths();
        
        // Log loaded public paths for debugging
        logger.info("SecurityFilter initialized with {} public paths", publicPaths.size());
        if (logger.isDebugEnabled()) {
            publicPaths.forEach(path -> logger.debug("  Public path: {}", path));
        }
    }
    
    /**
     * Validates the public paths configuration.
     * Requirements: 10.1, 10.2
     * 
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePublicPaths() {
        // Validate public paths are not empty
        if (publicPaths == null || publicPaths.isEmpty()) {
            throw new IllegalArgumentException(
                "Public paths configuration cannot be empty. At least authentication endpoints must be public.");
        }
        
        // Validate path patterns are valid
        for (String path : publicPaths) {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Public path cannot be null or empty");
            }
            
            // Validate path starts with /
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException(
                    String.format("Invalid public path '%s': must start with '/'", path));
            }
            
            // Validate wildcard patterns
            if (path.contains("*") && !path.endsWith("/**") && !path.endsWith("/*")) {
                throw new IllegalArgumentException(
                    String.format("Invalid public path '%s': wildcards must be at the end (/** or /*)", path));
            }
        }
        
        // Log warning if deprecated paths are used
        checkForDeprecatedPaths();
    }
    
    /**
     * Checks for deprecated path patterns and logs warnings.
     * Requirements: 10.2
     */
    private void checkForDeprecatedPaths() {
        // Check for deprecated H2 console path
        if (publicPaths.contains("/h2-console/**")) {
            logger.warn("Deprecated public path detected: '/h2-console/**' - " +
                "H2 console should not be exposed in production environments");
        }
        
        // Check for overly broad patterns
        if (publicPaths.contains("/api/**")) {
            logger.warn("Overly broad public path detected: '/api/**' - " +
                "This makes all API endpoints public. Consider using more specific paths.");
        }
        
        // Check for duplicate patterns
        long distinctCount = publicPaths.stream().distinct().count();
        if (distinctCount < publicPaths.size()) {
            logger.warn("Duplicate public paths detected in configuration. " +
                "Consider removing duplicates for clarity.");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Check if path is public (Requirements: 11.4, 11.5)
        if (isPublicPath(requestPath)) {
            // No authentication required, but create anonymous context
            SecurityContext anonymousContext = createAnonymousContext();

            SecurityContextHolder.runWithContext(anonymousContext, () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        // Protected path - require JWT authentication (Requirements: 11.1, 11.2, 11.3)
        try {
            String token = extractToken(httpRequest);
            
            // Validate JWT token using JwtToken.validate() (Requirements: 11.1, 11.2, 11.3)
            Result<JwtToken.JwtClaims> validationResult = JwtToken.validate(token, jwtSecret);
            
            if (validationResult.isFailure()) {
                // Handle validation failures with appropriate error responses
                handleAuthenticationFailure(httpResponse, requestPath, validationResult);
                return;
            }
            
            // Extract user details from JWT claims and populate SecurityContext (Requirements: 11.1)
            JwtToken.JwtClaims claims = validationResult.getValue().get();
            Authentication auth = createAuthenticationFromClaims(claims);
            SecurityContext securityContext = new DefaultSecurityContext(auth);

            SecurityContextHolder.runWithContext(securityContext, () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (MissingTokenException e) {
            // Handle missing token (Requirements: 11.3)
            logger.warn("Missing authentication token for path {}", requestPath);
            sendUnauthorizedResponse(httpResponse, "MISSING_TOKEN", "Authentication token is required");
        } catch (Exception e) {
            // Handle unexpected errors
            logger.error("Unexpected error during authentication for path {}: {}", requestPath, e.getMessage(), e);
            sendUnauthorizedResponse(httpResponse, "AUTHENTICATION_ERROR", "Authentication failed");
        }
    }
    
    /**
     * Creates an Authentication object from JWT claims.
     * Extracts user ID, bank ID (tenant context), and role from the token.
     * Requirements: 11.1
     */
    private Authentication createAuthenticationFromClaims(JwtToken.JwtClaims claims) {
        String userId = claims.getUserId();
        String email = claims.getEmail();
        
        // Try to extract tenant context (bankId and role) from claims
        // Tokens with tenant context have bankId and role directly in claims
        String bankId = extractBankId(claims);
        String role = extractRole(claims);
        
        // Create roles set from the extracted role
        Set<String> roles = role != null ? Set.of(role) : Set.of();
        
        // For now, permissions are empty - they can be loaded from database if needed
        Set<String> permissions = Set.of();
        
        if (bankId != null) {
            return new SimpleAuthentication(userId, bankId, permissions, roles);
        } else {
            return new SimpleAuthentication(userId, permissions, roles);
        }
    }
    
    /**
     * Extracts bank ID from JWT claims.
     * Handles both single-bank tokens (with bankId claim) and multi-bank tokens (with banks array).
     */
    private String extractBankId(JwtToken.JwtClaims claims) {
        try {
            // Try to get bankId directly (for tokens with selected tenant context)
            Object bankIdObj = claims.getClaims().get("bankId");
            if (bankIdObj != null) {
                return bankIdObj.toString();
            }
            
            // If no direct bankId, check if there's a banks array (multi-bank token)
            // In this case, we don't have a selected bank yet
            return null;
        } catch (Exception e) {
            logger.debug("Could not extract bankId from claims: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts role from JWT claims.
     */
    private String extractRole(JwtToken.JwtClaims claims) {
        try {
            Object roleObj = claims.getClaims().get("role");
            if (roleObj != null) {
                return roleObj.toString();
            }
            return null;
        } catch (Exception e) {
            logger.debug("Could not extract role from claims: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Handles authentication failures based on the validation result.
     * Requirements: 11.2, 11.3
     */
    private void handleAuthenticationFailure(
            HttpServletResponse response, 
            String requestPath, 
            Result<JwtToken.JwtClaims> validationResult) throws IOException {
        
        String errorCode = validationResult.getError().get().getCode();
        String errorMessage = validationResult.getError().get().getMessage();
        
        logger.warn("JWT validation failed for path {}: {} - {}", requestPath, errorCode, errorMessage);
        
        // Map error codes to appropriate responses
        switch (errorCode) {
            case "JWT_EXPIRED":
                sendUnauthorizedResponse(response, errorCode, "Access token has expired");
                break;
            case "JWT_INVALID_SIGNATURE":
                sendUnauthorizedResponse(response, errorCode, "Invalid token signature");
                break;
            case "JWT_MALFORMED":
                sendUnauthorizedResponse(response, errorCode, "Malformed token");
                break;
            default:
                sendUnauthorizedResponse(response, "INVALID_TOKEN", "Invalid authentication token");
                break;
        }
    }
    
    /**
     * Sends a 401 Unauthorized response with JSON error details.
     * Requirements: 11.2, 11.3
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"%s\",\"message\":\"%s\"}",
            errorCode,
            message.replace("\"", "\\\"")
        );
        
        response.getWriter().write(jsonResponse);
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream()
                .anyMatch(pattern -> matchesPattern(path, pattern));
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private SecurityContext createAnonymousContext() {
        Authentication anonymousAuth = AnonymousAuthentication.instance();
        return new DefaultSecurityContext(anonymousAuth);
    }

    /**
     * Extracts JWT token from Authorization header.
     * Requirements: 11.1
     * 
     * @throws MissingTokenException if token is missing or invalid format
     */
    private String extractToken(HttpServletRequest request) throws MissingTokenException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            throw new MissingTokenException("Authorization header is missing");
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            throw new MissingTokenException("Authorization header must start with 'Bearer '");
        }
        
        String token = authHeader.substring(7).trim();
        
        if (token.isEmpty()) {
            throw new MissingTokenException("Bearer token is empty");
        }

        return token;
    }
    
    /**
     * Exception thrown when authentication token is missing or invalid format.
     */
    private static class MissingTokenException extends Exception {
        public MissingTokenException(String message) {
            super(message);
        }
    }
}