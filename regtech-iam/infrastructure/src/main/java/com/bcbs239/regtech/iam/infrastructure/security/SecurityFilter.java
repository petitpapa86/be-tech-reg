package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.domain.security.Authentication;
import com.bcbs239.regtech.core.domain.security.SecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.AnonymousAuthentication;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.DefaultSecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.iam.domain.authentication.AuthenticationService;
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

/**
 * Security filter that handles authentication and sets up security context.
 * Uses Java 21 Scoped Values for thread-safe context management.
 * 
 * Public paths are loaded from configuration (iam.security.public-paths).
 * Requirements: 11.2, 11.3
 */
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final AuthenticationService authenticationService;
    private final List<String> publicPaths;

    /**
     * Constructor that accepts IAMProperties to load public paths from configuration.
     * 
     * @param authenticationService Service for authenticating users
     * @param iamProperties Configuration properties containing public paths
     * @throws IllegalArgumentException if public paths configuration is invalid
     */
    public SecurityFilter(AuthenticationService authenticationService, IAMProperties iamProperties) {
        this.authenticationService = authenticationService;
        this.publicPaths = iamProperties.getSecurity().getPublicPaths();
        
        // Validate public paths configuration (Requirements: 10.1, 10.2)
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

        // Check if path is public
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

        // Protected path - require authentication
        try {
            String token = extractToken(httpRequest);
            Authentication auth = authenticationService.authenticate(token);
            SecurityContext securityContext = new DefaultSecurityContext(auth);

            SecurityContextHolder.runWithContext(securityContext, () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (AuthenticationService.AuthenticationException e) {
            logger.warn("Authentication failed for path {}: {}", requestPath, e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" +
                    e.getMessage().replace("\"", "\\\"") + "\"}");
        }
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

    private String extractToken(HttpServletRequest request) throws AuthenticationService.AuthenticationException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        throw new AuthenticationService.AuthenticationException("Missing or invalid token");
    }
}