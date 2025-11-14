package com.bcbs239.regtech.iam.infrastructure.authentication;

import com.bcbs239.regtech.core.domain.security.Authentication;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SimpleAuthentication;
import com.bcbs239.regtech.iam.domain.authentication.AuthenticationService;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.domain.users.RolePermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT-based implementation of AuthenticationService.
 * Validates JWT tokens and extracts user authentication information.
 * Loads user data from database and resolves roles to permissions.
 */
@Service
public class JwtAuthenticationService implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationService.class);

    private final String jwtSecret;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RolePermissionService rolePermissionService;

    public JwtAuthenticationService(
            @Value("${regtech.jwt.secret:default-secret-key}") String jwtSecret,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            RolePermissionService rolePermissionService) {
        this.jwtSecret = jwtSecret;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public Authentication authenticate(String token) throws AuthenticationException {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new AuthenticationException("Token is required");
            }

            // Parse and validate JWT token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new AuthenticationException("Invalid JWT format");
            }

            // Verify signature
            if (!verifySignature(token)) {
                throw new AuthenticationException("Invalid token signature");
            }

            // Check expiration
            JsonNode payload = parsePayload(parts[1]);
            if (payload == null) {
                throw new AuthenticationException("Invalid token payload");
            }

            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                throw new AuthenticationException("Token has expired");
            }

            // Extract user ID from token
            String userId = extractUserIdFromPayload(payload);
            if (userId == null) {
                throw new AuthenticationException("User ID not found in token");
            }

            // Load user from database
            UserId userIdObj = UserId.fromString(userId);
            Maybe<User> userMaybe = userRepository.userLoader(userIdObj);
            if (userMaybe.isEmpty()) {
                throw new AuthenticationException("User not found: " + userId);
            }

            User user = userMaybe.getValue();

            // Check if user is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AuthenticationException("User account is not active");
            }

            // Extract permissions and roles from user's bank assignments
            Set<String> permissions = new HashSet<>();
            Set<String> roles = new HashSet<>();

            for (User.BankAssignment assignment : user.getBankAssignments()) {
                String roleName = assignment.getRole().toUpperCase();

                // Load permissions for this role using the service
                var permissionResult = rolePermissionService.loadPermissions(roleName);
                if (permissionResult.isSuccess()) {
                    permissions.addAll(permissionResult.getValue().orElseThrow());
                } else {
                    logger.warn("Failed to load permissions for role {}: {}", roleName, permissionResult.getError());
                }

                roles.add(roleName.toLowerCase());
            }

            return new SimpleAuthentication(userId, permissions, roles);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isValidToken(String token) {
        try {
            authenticate(token);
            return true;
        } catch (AuthenticationException e) {
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        try {
            if (!isValidToken(token)) {
                return null;
            }

            String[] parts = token.split("\\.");
            JsonNode payload = parsePayload(parts[1]);
            return extractUserIdFromPayload(payload);
        } catch (Exception e) {
            logger.debug("Error extracting user ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verify JWT signature using HMAC SHA-256.
     */
    private boolean verifySignature(String token) {
        try {
            String[] parts = token.split("\\.");
            String headerAndPayload = parts[0] + "." + parts[1];
            String signature = parts[2];

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] expectedSignature = mac.doFinal(headerAndPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSignature);

            return expectedSignatureBase64.equals(signature);

        } catch (Exception e) {
            logger.debug("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse JWT payload from base64 encoded string.
     */
    private JsonNode parsePayload(String encodedPayload) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPayload);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            return objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            logger.debug("Error parsing JWT payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID from JWT payload.
     */
    private String extractUserIdFromPayload(JsonNode payload) {
        // Try different common claims for user ID
        String userId = payload.path("userId").asText(null);
        if (userId == null) {
            userId = payload.path("sub").asText(null);
        }
        if (userId == null) {
            userId = payload.path("user_id").asText(null);
        }
        if (userId == null) {
            userId = payload.path("id").asText(null);
        }
        return userId;
    }
}