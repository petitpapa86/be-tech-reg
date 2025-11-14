package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.domain.security.Authentication;
import com.bcbs239.regtech.core.domain.security.SecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.AnonymousAuthentication;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.DefaultSecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.iam.domain.authentication.AuthenticationService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Security filter that handles authentication and sets up security context.
 * Uses Java 21 Scoped Values for thread-safe context management.
 */
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final AuthenticationService authenticationService;
    private final Set<String> publicPaths;

    public SecurityFilter(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        // Define public paths that don't require authentication
        this.publicPaths = Set.of(
            "/api/public/**",
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/v1/users/register",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
        );
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