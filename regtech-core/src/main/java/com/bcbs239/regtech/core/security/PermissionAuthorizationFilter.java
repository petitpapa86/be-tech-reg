package com.bcbs239.regtech.core.security;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Permission-based authorization filter that validates user permissions against endpoint requirements.
 * Integrates with JWT tokens to extract user permissions and validate against route attributes.
 */
@Component
@Order(2) // After authentication filter
public class PermissionAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAuthorizationFilter.class);
    
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    public PermissionAuthorizationFilter(PermissionService permissionService, ObjectMapper objectMapper) {
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authorization for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Authorization header
            String authToken = extractAuthToken(request);
            if (authToken == null) {
                sendUnauthorizedResponse(response, "Missing or invalid authorization token");
                return;
            }

            // Get required permissions for this endpoint
            String[] requiredPermissions = getRequiredPermissions(request);
            if (requiredPermissions == null || requiredPermissions.length == 0) {
                // No specific permissions required, but token must be valid
                if (!permissionService.isValidToken(authToken)) {
                    sendUnauthorizedResponse(response, "Invalid authorization token");
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }

            // Validate user permissions
            Set<String> userPermissions = permissionService.getUserPermissions(authToken);
            if (userPermissions == null) {
                sendUnauthorizedResponse(response, "Unable to retrieve user permissions");
                return;
            }

            // Check if user has all required permissions
            for (String requiredPermission : requiredPermissions) {
                if (!userPermissions.contains(requiredPermission)) {
                    sendForbiddenResponse(response, 
                        String.format("Access denied. Required permission: %s", requiredPermission));
                    return;
                }
            }

            // Extract additional user context
            String bankId = permissionService.getBankId(authToken);
            String userId = permissionService.getUserId(authToken);

            // Set security context for downstream use
            SecurityContext.setSecurityContext(authToken, userPermissions, bankId, userId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clear security context after request processing
                SecurityContext.clearSecurityContext();
            }

        } catch (Exception e) {
            logger.error("Error during permission authorization: {}", e.getMessage(), e);
            sendInternalErrorResponse(response, "Authorization check failed");
        }
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractAuthToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader; // Return as-is for other token formats
    }

    /**
     * Get required permissions from route attributes.
     */
    private String[] getRequiredPermissions(HttpServletRequest request) {
        // Try to get from request attributes (set by RouterFunction)
        Object permissions = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (permissions instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) permissions;
            Object permissionAttr = attributes.get("permissions");
            if (permissionAttr instanceof String[]) {
                return (String[]) permissionAttr;
            }
        }

        // Fallback: extract from request path patterns
        return extractPermissionsFromPath(request.getRequestURI(), request.getMethod());
    }

    /**
     * Extract permissions based on URL patterns and HTTP methods.
     */
    private String[] extractPermissionsFromPath(String path, String method) {
        // Ingestion module permissions
        if (path.startsWith("/api/v1/ingestion/")) {
            if (path.contains("/upload") && "POST".equals(method)) {
                return new String[]{"ingestion:upload"};
            }
            if (path.contains("/status") && "GET".equals(method)) {
                return new String[]{"ingestion:status:view"};
            }
            if (path.contains("/process") && "POST".equals(method)) {
                return new String[]{"ingestion:process"};
            }
        }

        // Billing module permissions
        if (path.startsWith("/api/v1/billing/")) {
            if ("GET".equals(method)) {
                return new String[]{"billing:read"};
            }
            if ("POST".equals(method) || "PUT".equals(method)) {
                return new String[]{"billing:write"};
            }
        }

        // IAM module permissions
        if (path.startsWith("/api/v1/users/")) {
            if ("POST".equals(method)) {
                return new String[]{"users:create"};
            }
            if ("GET".equals(method)) {
                return new String[]{"users:read"};
            }
        }

        // Default: require authentication but no specific permissions
        return new String[]{};
    }

    /**
     * Check if endpoint is public (no authentication required).
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Health check endpoints
        if (path.equals("/health") || path.equals("/actuator/health")) {
            return true;
        }

        // API documentation endpoints
        if (path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        // Static resources
        if (path.startsWith("/static/") || path.startsWith("/css/") || path.startsWith("/js/")) {
            return true;
        }

        return false;
    }

    /**
     * Send 401 Unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        ApiResponse<Void> errorResponse = ResponseUtils.authenticationError(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Send 403 Forbidden response.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        
        ApiResponse<Void> errorResponse = ResponseUtils.authenticationError(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Send 500 Internal Server Error response.
     */
    private void sendInternalErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        
        ApiResponse<Void> errorResponse = ResponseUtils.systemError(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}