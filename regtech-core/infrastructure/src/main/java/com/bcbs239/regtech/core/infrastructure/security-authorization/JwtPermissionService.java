package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.fasterxml.jackson.databind.JsonNode;
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
 * JWT-based implementation of PermissionService.
 * Validates JWT tokens and extracts user permissions and bank information.
 */
@Service
public class JwtPermissionService implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(JwtPermissionService.class);
    
    private final String jwtSecret;
    private final ObjectMapper objectMapper;

    public JwtPermissionService(
            @Value("${regtech.jwt.secret:default-secret-key}") String jwtSecret,
            ObjectMapper objectMapper) {
        this.jwtSecret = jwtSecret;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isValidToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            // Parse JWT token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.debug("Invalid JWT format: expected 3 parts, got {}", parts.length);
                return false;
            }

            // Verify signature
            if (!verifySignature(token)) {
                logger.debug("JWT signature verification failed");
                return false;
            }

            // Check expiration
            JsonNode payload = parsePayload(parts[1]);
            if (payload == null) {
                return false;
            }

            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                logger.debug("JWT token has expired");
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.debug("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Set<String> getUserPermissions(String token) {
        try {
            if (!isValidToken(token)) {
                return null;
            }

            String[] parts = token.split("\\.");
            JsonNode payload = parsePayload(parts[1]);
            if (payload == null) {
                return null;
            }

            Set<String> permissions = new HashSet<>();

            // Extract permissions from 'permissions' claim
            JsonNode permissionsNode = payload.path("permissions");
            if (permissionsNode.isArray()) {
                for (JsonNode permission : permissionsNode) {
                    permissions.add(permission.asText());
                }
            }

            // Extract permissions from 'roles' claim and map to permissions
            JsonNode rolesNode = payload.path("roles");
            if (rolesNode.isArray()) {
                for (JsonNode role : rolesNode) {
                    permissions.addAll(mapRoleToPermissions(role.asText()));
                }
            }

            // Add bank-specific permissions based on bank_id
            String bankId = payload.path("bank_id").asText(null);
            if (bankId != null) {
                permissions.addAll(getBankSpecificPermissions(bankId));
            }

            return permissions;

        } catch (Exception e) {
            logger.error("Error extracting user permissions: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getBankId(String token) {
        try {
            if (!isValidToken(token)) {
                return null;
            }

            String[] parts = token.split("\\.");
            JsonNode payload = parsePayload(parts[1]);
            if (payload == null) {
                return null;
            }

            return payload.path("bank_id").asText(null);

        } catch (Exception e) {
            logger.error("Error extracting bank ID: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getUserId(String token) {
        try {
            if (!isValidToken(token)) {
                return null;
            }

            String[] parts = token.split("\\.");
            JsonNode payload = parsePayload(parts[1]);
            if (payload == null) {
                return null;
            }

            // Try different common claims for user ID
            String userId = payload.path("sub").asText(null);
            if (userId == null) {
                userId = payload.path("user_id").asText(null);
            }
            if (userId == null) {
                userId = payload.path("id").asText(null);
            }

            return userId;

        } catch (Exception e) {
            logger.error("Error extracting user ID: {}", e.getMessage(), e);
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
     * Map user roles to specific permissions.
     */
    private Set<String> mapRoleToPermissions(String role) {
        Set<String> permissions = new HashSet<>();

        switch (role.toLowerCase()) {
            case "admin":
                permissions.add("ingestion:upload");
                permissions.add("ingestion:status:view");
                permissions.add("ingestion:process");
                permissions.add("billing:read");
                permissions.add("billing:write");
                permissions.add("users:create");
                permissions.add("users:read");
                permissions.add("users:update");
                permissions.add("users:delete");
                break;

            case "bank_user":
                permissions.add("ingestion:upload");
                permissions.add("ingestion:status:view");
                permissions.add("billing:read");
                permissions.add("users:read");
                break;

            case "bank_admin":
                permissions.add("ingestion:upload");
                permissions.add("ingestion:status:view");
                permissions.add("ingestion:process");
                permissions.add("billing:read");
                permissions.add("billing:write");
                permissions.add("users:create");
                permissions.add("users:read");
                permissions.add("users:update");
                break;

            case "readonly":
                permissions.add("ingestion:status:view");
                permissions.add("billing:read");
                permissions.add("users:read");
                break;

            default:
                logger.debug("Unknown role: {}", role);
                break;
        }

        return permissions;
    }

    /**
     * Get bank-specific permissions based on bank configuration.
     */
    private Set<String> getBankSpecificPermissions(String bankId) {
        Set<String> permissions = new HashSet<>();

        // In a real implementation, this would query a database or configuration service
        // For now, we'll provide basic permissions for all banks
        permissions.add("ingestion:upload");
        permissions.add("ingestion:status:view");
        permissions.add("billing:read");

        // Special permissions for specific banks (example)
        if ("BANK001".equals(bankId) || "BANK002".equals(bankId)) {
            permissions.add("ingestion:process");
            permissions.add("billing:write");
        }

        return permissions;
    }
}
