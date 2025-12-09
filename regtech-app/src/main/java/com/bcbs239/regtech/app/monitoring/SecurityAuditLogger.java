package com.bcbs239.regtech.app.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Security Audit Logger for authentication, authorization, and security-relevant events.
 * 
 * This service provides comprehensive security audit logging with automatic trace context
 * integration via Spring Boot 4's Micrometer Tracing. All audit logs automatically include
 * trace IDs and span IDs for correlation with distributed traces.
 * 
 * Requirements: 7.1, 7.2, 7.4 - Security audit logging
 */
@Component
public class SecurityAuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLogger.class);
    
    private final TraceContextManager traceContextManager;

    public SecurityAuditLogger(TraceContextManager traceContextManager) {
        this.traceContextManager = traceContextManager;
    }

    /**
     * Logs an authentication attempt with user identity and outcome.
     * 
     * @param userId The user ID attempting authentication
     * @param authenticationMethod The authentication method used (e.g., "password", "oauth2", "refresh_token")
     * @param success Whether the authentication was successful
     * @param details Additional details about the authentication attempt
     * 
     * Requirements: 7.1 - Authentication logging
     */
    public void logAuthentication(String userId, String authenticationMethod, boolean success, String details) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "authentication");
        auditData.put("user_id", userId);
        auditData.put("authentication_method", authenticationMethod);
        auditData.put("success", success);
        auditData.put("outcome", success ? "SUCCESS" : "FAILURE");
        auditData.put("details", details);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "authentication");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.outcome", success ? "success" : "failure");
        }
        
        // Log with appropriate level
        if (success) {
            auditLogger.info("Authentication attempt: {}", formatAuditData(auditData));
        } else {
            auditLogger.warn("Failed authentication attempt: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged authentication event for user: {}, success: {}", userId, success);
    }

    /**
     * Logs an authentication attempt with IP address and user agent.
     * 
     * @param userId The user ID attempting authentication
     * @param authenticationMethod The authentication method used
     * @param success Whether the authentication was successful
     * @param ipAddress The IP address of the authentication attempt
     * @param userAgent The user agent string
     * @param details Additional details
     * 
     * Requirements: 7.1 - Authentication logging with context
     */
    public void logAuthenticationWithContext(String userId, String authenticationMethod, boolean success, 
                                            String ipAddress, String userAgent, String details) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "authentication");
        auditData.put("user_id", userId);
        auditData.put("authentication_method", authenticationMethod);
        auditData.put("success", success);
        auditData.put("outcome", success ? "SUCCESS" : "FAILURE");
        auditData.put("ip_address", ipAddress);
        auditData.put("user_agent", userAgent);
        auditData.put("details", details);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "authentication");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.outcome", success ? "success" : "failure");
            traceContextManager.addBusinessContext("security.ip_address", ipAddress);
        }
        
        // Log with appropriate level
        if (success) {
            auditLogger.info("Authentication attempt with context: {}", formatAuditData(auditData));
        } else {
            auditLogger.warn("Failed authentication attempt with context: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged authentication event with context for user: {}, success: {}", userId, success);
    }

    /**
     * Logs an authorization decision with resource details.
     * 
     * @param userId The user ID making the request
     * @param resource The resource being accessed
     * @param action The action being performed (e.g., "read", "write", "delete")
     * @param granted Whether access was granted
     * @param reason The reason for the decision
     * 
     * Requirements: 7.2 - Authorization decision recording
     */
    public void logAuthorization(String userId, String resource, String action, boolean granted, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "authorization");
        auditData.put("user_id", userId);
        auditData.put("resource", resource);
        auditData.put("action", action);
        auditData.put("granted", granted);
        auditData.put("outcome", granted ? "GRANTED" : "DENIED");
        auditData.put("reason", reason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "authorization");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.resource", resource);
            traceContextManager.addBusinessContext("security.action", action);
            traceContextManager.addBusinessContext("security.outcome", granted ? "granted" : "denied");
        }
        
        // Log with appropriate level
        if (granted) {
            auditLogger.info("Authorization decision: {}", formatAuditData(auditData));
        } else {
            auditLogger.warn("Authorization denied: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged authorization event for user: {}, resource: {}, granted: {}", userId, resource, granted);
    }

    /**
     * Logs an authorization decision with role and permission details.
     * 
     * @param userId The user ID making the request
     * @param userRole The user's role
     * @param resource The resource being accessed
     * @param action The action being performed
     * @param requiredPermission The permission required for the action
     * @param granted Whether access was granted
     * @param reason The reason for the decision
     * 
     * Requirements: 7.2 - Authorization decision recording with details
     */
    public void logAuthorizationWithPermissions(String userId, String userRole, String resource, String action, 
                                               String requiredPermission, boolean granted, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "authorization");
        auditData.put("user_id", userId);
        auditData.put("user_role", userRole);
        auditData.put("resource", resource);
        auditData.put("action", action);
        auditData.put("required_permission", requiredPermission);
        auditData.put("granted", granted);
        auditData.put("outcome", granted ? "GRANTED" : "DENIED");
        auditData.put("reason", reason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "authorization");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.user.role", userRole);
            traceContextManager.addBusinessContext("security.resource", resource);
            traceContextManager.addBusinessContext("security.action", action);
            traceContextManager.addBusinessContext("security.outcome", granted ? "granted" : "denied");
        }
        
        // Log with appropriate level
        if (granted) {
            auditLogger.info("Authorization decision with permissions: {}", formatAuditData(auditData));
        } else {
            auditLogger.warn("Authorization denied with permissions: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged authorization event with permissions for user: {}, resource: {}, granted: {}", 
                    userId, resource, granted);
    }

    /**
     * Logs an administrative action with change tracking.
     * 
     * @param adminUserId The administrator user ID
     * @param actionType The type of administrative action (e.g., "user_create", "role_update", "config_change")
     * @param targetEntity The entity being modified (e.g., "user", "role", "configuration")
     * @param targetId The ID of the target entity
     * @param changeDetails Details about the changes made
     * 
     * Requirements: 7.4 - Administrative action auditing
     */
    public void logAdministrativeAction(String adminUserId, String actionType, String targetEntity, 
                                       String targetId, String changeDetails) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "administrative_action");
        auditData.put("admin_user_id", adminUserId);
        auditData.put("action_type", actionType);
        auditData.put("target_entity", targetEntity);
        auditData.put("target_id", targetId);
        auditData.put("change_details", changeDetails);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "administrative_action");
            traceContextManager.addBusinessContext("security.admin.user.id", adminUserId);
            traceContextManager.addBusinessContext("security.action.type", actionType);
            traceContextManager.addBusinessContext("security.target.entity", targetEntity);
            traceContextManager.addBusinessContext("security.target.id", targetId);
        }
        
        auditLogger.warn("Administrative action: {}", formatAuditData(auditData));
        
        logger.debug("Logged administrative action by user: {}, action: {}, target: {}", 
                    adminUserId, actionType, targetEntity);
    }

    /**
     * Logs an administrative action with before and after values.
     * 
     * @param adminUserId The administrator user ID
     * @param actionType The type of administrative action
     * @param targetEntity The entity being modified
     * @param targetId The ID of the target entity
     * @param beforeValue The value before the change
     * @param afterValue The value after the change
     * @param changeReason The reason for the change
     * 
     * Requirements: 7.4 - Administrative action auditing with change tracking
     */
    public void logAdministrativeActionWithChanges(String adminUserId, String actionType, String targetEntity, 
                                                  String targetId, String beforeValue, String afterValue, 
                                                  String changeReason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "administrative_action");
        auditData.put("admin_user_id", adminUserId);
        auditData.put("action_type", actionType);
        auditData.put("target_entity", targetEntity);
        auditData.put("target_id", targetId);
        auditData.put("before_value", beforeValue);
        auditData.put("after_value", afterValue);
        auditData.put("change_reason", changeReason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "administrative_action");
            traceContextManager.addBusinessContext("security.admin.user.id", adminUserId);
            traceContextManager.addBusinessContext("security.action.type", actionType);
            traceContextManager.addBusinessContext("security.target.entity", targetEntity);
            traceContextManager.addBusinessContext("security.target.id", targetId);
        }
        
        auditLogger.warn("Administrative action with changes: {}", formatAuditData(auditData));
        
        logger.debug("Logged administrative action with changes by user: {}, action: {}, target: {}", 
                    adminUserId, actionType, targetEntity);
    }

    /**
     * Logs a configuration change event.
     * 
     * @param adminUserId The administrator user ID
     * @param configKey The configuration key being changed
     * @param oldValue The old configuration value
     * @param newValue The new configuration value
     * @param reason The reason for the change
     * 
     * Requirements: 7.4 - Configuration change auditing
     */
    public void logConfigurationChange(String adminUserId, String configKey, String oldValue, 
                                      String newValue, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "configuration_change");
        auditData.put("admin_user_id", adminUserId);
        auditData.put("config_key", configKey);
        auditData.put("old_value", oldValue);
        auditData.put("new_value", newValue);
        auditData.put("reason", reason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "configuration_change");
            traceContextManager.addBusinessContext("security.admin.user.id", adminUserId);
            traceContextManager.addBusinessContext("security.config.key", configKey);
        }
        
        auditLogger.warn("Configuration change: {}", formatAuditData(auditData));
        
        logger.debug("Logged configuration change by user: {}, key: {}", adminUserId, configKey);
    }

    /**
     * Logs a user logout event.
     * 
     * @param userId The user ID logging out
     * @param sessionDuration The duration of the session in seconds
     * @param reason The reason for logout (e.g., "user_initiated", "session_timeout", "forced")
     * 
     * Requirements: 7.1 - Authentication lifecycle logging
     */
    public void logLogout(String userId, Long sessionDuration, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "logout");
        auditData.put("user_id", userId);
        auditData.put("session_duration_seconds", sessionDuration);
        auditData.put("reason", reason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "logout");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.logout.reason", reason);
        }
        
        auditLogger.info("User logout: {}", formatAuditData(auditData));
        
        logger.debug("Logged logout event for user: {}, reason: {}", userId, reason);
    }

    /**
     * Logs a session expiration event.
     * 
     * @param userId The user ID whose session expired
     * @param sessionId The session ID that expired
     * @param expirationReason The reason for expiration (e.g., "timeout", "max_age", "revoked")
     * 
     * Requirements: 7.1 - Session management logging
     */
    public void logSessionExpiration(String userId, String sessionId, String expirationReason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "session_expiration");
        auditData.put("user_id", userId);
        auditData.put("session_id", sessionId);
        auditData.put("expiration_reason", expirationReason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "session_expiration");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.session.id", sessionId);
        }
        
        auditLogger.info("Session expiration: {}", formatAuditData(auditData));
        
        logger.debug("Logged session expiration for user: {}, reason: {}", userId, expirationReason);
    }

    /**
     * Logs a password change event.
     * 
     * @param userId The user ID changing password
     * @param initiatedBy Who initiated the change (e.g., "user", "admin", "system")
     * @param success Whether the password change was successful
     * @param reason The reason for the change
     * 
     * Requirements: 7.1 - Credential management logging
     */
    public void logPasswordChange(String userId, String initiatedBy, boolean success, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "password_change");
        auditData.put("user_id", userId);
        auditData.put("initiated_by", initiatedBy);
        auditData.put("success", success);
        auditData.put("outcome", success ? "SUCCESS" : "FAILURE");
        auditData.put("reason", reason);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "password_change");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.initiated.by", initiatedBy);
            traceContextManager.addBusinessContext("security.outcome", success ? "success" : "failure");
        }
        
        auditLogger.warn("Password change: {}", formatAuditData(auditData));
        
        logger.debug("Logged password change for user: {}, success: {}", userId, success);
    }

    // Sensitive Data Access Logging Methods

    /**
     * Logs sensitive data access with user context and data classification.
     * 
     * @param userId The user ID accessing the data
     * @param dataType The type of sensitive data (e.g., "pii", "financial", "confidential")
     * @param dataClassification The data classification level (e.g., "public", "internal", "confidential", "restricted")
     * @param identifier The identifier of the data being accessed (e.g., record ID, file name)
     * @param accessType The type of access (e.g., "read", "write", "export", "delete")
     * @param purpose The business purpose for accessing the data
     * 
     * Requirements: 7.3 - Sensitive data access logging
     */
    public void logSensitiveDataAccess(String userId, String dataType, String dataClassification, 
                                      String identifier, String accessType, String purpose) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "sensitive_data_access");
        auditData.put("user_id", userId);
        auditData.put("data_type", dataType);
        auditData.put("data_classification", dataClassification);
        auditData.put("identifier", identifier);
        auditData.put("access_type", accessType);
        auditData.put("purpose", purpose);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "sensitive_data_access");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.data.type", dataType);
            traceContextManager.addBusinessContext("security.data.classification", dataClassification);
            traceContextManager.addBusinessContext("security.access.type", accessType);
        }
        
        auditLogger.warn("Sensitive data access: {}", formatAuditData(auditData));
        
        logger.debug("Logged sensitive data access by user: {}, type: {}, classification: {}", 
                    userId, dataType, dataClassification);
    }

    /**
     * Logs PII (Personally Identifiable Information) access.
     * 
     * @param userId The user ID accessing the PII
     * @param piiType The type of PII (e.g., "name", "email", "ssn", "address", "phone")
     * @param subjectUserId The user ID whose PII is being accessed
     * @param accessType The type of access
     * @param purpose The business purpose for accessing the PII
     * 
     * Requirements: 7.3 - PII access logging
     */
    public void logPiiAccess(String userId, String piiType, String subjectUserId, 
                            String accessType, String purpose) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "pii_access");
        auditData.put("user_id", userId);
        auditData.put("pii_type", piiType);
        auditData.put("subject_user_id", subjectUserId);
        auditData.put("access_type", accessType);
        auditData.put("purpose", purpose);
        auditData.put("data_classification", "restricted");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "pii_access");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.pii.type", piiType);
            traceContextManager.addBusinessContext("security.subject.user.id", subjectUserId);
            traceContextManager.addBusinessContext("security.access.type", accessType);
        }
        
        auditLogger.warn("PII access: {}", formatAuditData(auditData));
        
        logger.debug("Logged PII access by user: {}, type: {}, subject: {}", userId, piiType, subjectUserId);
    }

    /**
     * Logs financial data access.
     * 
     * @param userId The user ID accessing the financial data
     * @param financialDataType The type of financial data (e.g., "account", "transaction", "balance", "payment")
     * @param accountId The account ID or identifier
     * @param accessType The type of access
     * @param purpose The business purpose for accessing the data
     * 
     * Requirements: 7.3 - Financial data access logging
     */
    public void logFinancialDataAccess(String userId, String financialDataType, String accountId, 
                                      String accessType, String purpose) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "financial_data_access");
        auditData.put("user_id", userId);
        auditData.put("financial_data_type", financialDataType);
        auditData.put("account_id", accountId);
        auditData.put("access_type", accessType);
        auditData.put("purpose", purpose);
        auditData.put("data_classification", "confidential");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "financial_data_access");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.financial.data.type", financialDataType);
            traceContextManager.addBusinessContext("security.account.id", accountId);
            traceContextManager.addBusinessContext("security.access.type", accessType);
        }
        
        auditLogger.warn("Financial data access: {}", formatAuditData(auditData));
        
        logger.debug("Logged financial data access by user: {}, type: {}, account: {}", 
                    userId, financialDataType, accountId);
    }

    /**
     * Logs bulk data export with automatic detection of sensitive data patterns.
     * 
     * @param userId The user ID performing the export
     * @param exportType The type of export (e.g., "csv", "json", "pdf", "excel")
     * @param recordCount The number of records being exported
     * @param dataTypes The types of data included in the export
     * @param containsSensitiveData Whether the export contains sensitive data
     * @param purpose The business purpose for the export
     * 
     * Requirements: 7.3 - Bulk data export logging with sensitive data detection
     */
    public void logBulkDataExport(String userId, String exportType, int recordCount, 
                                 String dataTypes, boolean containsSensitiveData, String purpose) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "bulk_data_export");
        auditData.put("user_id", userId);
        auditData.put("export_type", exportType);
        auditData.put("record_count", recordCount);
        auditData.put("data_types", dataTypes);
        auditData.put("contains_sensitive_data", containsSensitiveData);
        auditData.put("purpose", purpose);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "bulk_data_export");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.export.type", exportType);
            traceContextManager.addBusinessContext("security.record.count", String.valueOf(recordCount));
            traceContextManager.addBusinessContext("security.contains.sensitive.data", 
                                                  String.valueOf(containsSensitiveData));
        }
        
        // Use higher severity for sensitive data exports
        if (containsSensitiveData) {
            auditLogger.warn("Bulk data export with sensitive data: {}", formatAuditData(auditData));
        } else {
            auditLogger.info("Bulk data export: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged bulk data export by user: {}, records: {}, sensitive: {}", 
                    userId, recordCount, containsSensitiveData);
    }

    /**
     * Logs data masking or anonymization operations.
     * 
     * @param userId The user ID performing the operation
     * @param operationType The type of operation (e.g., "mask", "anonymize", "pseudonymize")
     * @param dataType The type of data being processed
     * @param recordCount The number of records processed
     * @param maskingMethod The masking method used
     * 
     * Requirements: 7.3 - Data protection operation logging
     */
    public void logDataMaskingOperation(String userId, String operationType, String dataType, 
                                       int recordCount, String maskingMethod) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "data_masking_operation");
        auditData.put("user_id", userId);
        auditData.put("operation_type", operationType);
        auditData.put("data_type", dataType);
        auditData.put("record_count", recordCount);
        auditData.put("masking_method", maskingMethod);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "data_masking_operation");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.operation.type", operationType);
            traceContextManager.addBusinessContext("security.data.type", dataType);
        }
        
        auditLogger.info("Data masking operation: {}", formatAuditData(auditData));
        
        logger.debug("Logged data masking operation by user: {}, type: {}, records: {}", 
                    userId, operationType, recordCount);
    }

    /**
     * Automatically detects and logs sensitive data access patterns.
     * This method analyzes access patterns and logs suspicious behavior.
     * 
     * @param userId The user ID
     * @param accessPattern The detected access pattern (e.g., "bulk_access", "unusual_time", "unusual_location")
     * @param dataType The type of data accessed
     * @param accessCount The number of accesses in the pattern
     * @param timeWindow The time window for the pattern (e.g., "5 minutes", "1 hour")
     * @param riskLevel The risk level of the pattern (e.g., "low", "medium", "high")
     * 
     * Requirements: 7.3 - Automatic detection of sensitive data access patterns
     */
    public void logSensitiveDataAccessPattern(String userId, String accessPattern, String dataType, 
                                             int accessCount, String timeWindow, String riskLevel) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "sensitive_data_access_pattern");
        auditData.put("user_id", userId);
        auditData.put("access_pattern", accessPattern);
        auditData.put("data_type", dataType);
        auditData.put("access_count", accessCount);
        auditData.put("time_window", timeWindow);
        auditData.put("risk_level", riskLevel);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "sensitive_data_access_pattern");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.access.pattern", accessPattern);
            traceContextManager.addBusinessContext("security.risk.level", riskLevel);
        }
        
        // Use severity based on risk level
        if ("high".equalsIgnoreCase(riskLevel)) {
            auditLogger.error("High-risk sensitive data access pattern detected: {}", formatAuditData(auditData));
        } else if ("medium".equalsIgnoreCase(riskLevel)) {
            auditLogger.warn("Medium-risk sensitive data access pattern detected: {}", formatAuditData(auditData));
        } else {
            auditLogger.info("Sensitive data access pattern detected: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged sensitive data access pattern for user: {}, pattern: {}, risk: {}", 
                    userId, accessPattern, riskLevel);
    }

    // Security Violation Detection and Logging Methods

    /**
     * Logs a security violation with threat indicators.
     * 
     * @param userId The user ID associated with the violation (may be null for anonymous)
     * @param violationType The type of security violation (e.g., "unauthorized_access", "injection_attempt", "brute_force")
     * @param severity The severity level (e.g., "low", "medium", "high", "critical")
     * @param threatIndicators Contextual information about the threat
     * @param ipAddress The IP address associated with the violation
     * @param actionTaken The action taken in response (e.g., "blocked", "logged", "alerted")
     * 
     * Requirements: 7.5 - Security violation logging with threat indicators
     */
    public void logSecurityViolation(String userId, String violationType, String severity, 
                                    String threatIndicators, String ipAddress, String actionTaken) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "security_violation");
        auditData.put("user_id", userId != null ? userId : "anonymous");
        auditData.put("violation_type", violationType);
        auditData.put("severity", severity);
        auditData.put("threat_indicators", threatIndicators);
        auditData.put("ip_address", ipAddress);
        auditData.put("action_taken", actionTaken);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "security_violation");
            traceContextManager.addBusinessContext("security.violation.type", violationType);
            traceContextManager.addBusinessContext("security.severity", severity);
            traceContextManager.addBusinessContext("security.action.taken", actionTaken);
            if (userId != null) {
                traceContextManager.addBusinessContext("security.user.id", userId);
            }
        }
        
        // Use severity-based logging
        if ("critical".equalsIgnoreCase(severity)) {
            auditLogger.error("CRITICAL security violation: {}", formatAuditData(auditData));
        } else if ("high".equalsIgnoreCase(severity)) {
            auditLogger.error("HIGH security violation: {}", formatAuditData(auditData));
        } else if ("medium".equalsIgnoreCase(severity)) {
            auditLogger.warn("MEDIUM security violation: {}", formatAuditData(auditData));
        } else {
            auditLogger.warn("Security violation: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged security violation: type={}, severity={}", violationType, severity);
    }

    /**
     * Logs a brute force attack attempt.
     * 
     * @param targetUserId The user ID being targeted (may be null)
     * @param attemptCount The number of failed attempts
     * @param timeWindow The time window for the attempts
     * @param ipAddress The IP address of the attacker
     * @param actionTaken The action taken (e.g., "account_locked", "ip_blocked", "rate_limited")
     * 
     * Requirements: 7.5 - Brute force attack detection
     */
    public void logBruteForceAttempt(String targetUserId, int attemptCount, String timeWindow, 
                                    String ipAddress, String actionTaken) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "brute_force_attempt");
        auditData.put("target_user_id", targetUserId != null ? targetUserId : "unknown");
        auditData.put("attempt_count", attemptCount);
        auditData.put("time_window", timeWindow);
        auditData.put("ip_address", ipAddress);
        auditData.put("action_taken", actionTaken);
        auditData.put("severity", "high");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "brute_force_attempt");
            traceContextManager.addBusinessContext("security.attempt.count", String.valueOf(attemptCount));
            traceContextManager.addBusinessContext("security.ip.address", ipAddress);
            traceContextManager.addBusinessContext("security.action.taken", actionTaken);
        }
        
        auditLogger.error("Brute force attack detected: {}", formatAuditData(auditData));
        
        logger.debug("Logged brute force attempt: attempts={}, ip={}", attemptCount, ipAddress);
    }

    /**
     * Logs a SQL injection attempt.
     * 
     * @param userId The user ID associated with the attempt (may be null)
     * @param injectionPattern The detected injection pattern
     * @param targetQuery The query that was targeted
     * @param ipAddress The IP address of the attacker
     * @param blocked Whether the attempt was blocked
     * 
     * Requirements: 7.5 - Injection attack detection
     */
    public void logSqlInjectionAttempt(String userId, String injectionPattern, String targetQuery, 
                                      String ipAddress, boolean blocked) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "sql_injection_attempt");
        auditData.put("user_id", userId != null ? userId : "anonymous");
        auditData.put("injection_pattern", injectionPattern);
        auditData.put("target_query", targetQuery);
        auditData.put("ip_address", ipAddress);
        auditData.put("blocked", blocked);
        auditData.put("severity", "critical");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "sql_injection_attempt");
            traceContextManager.addBusinessContext("security.injection.pattern", injectionPattern);
            traceContextManager.addBusinessContext("security.blocked", String.valueOf(blocked));
            if (userId != null) {
                traceContextManager.addBusinessContext("security.user.id", userId);
            }
        }
        
        auditLogger.error("SQL injection attempt detected: {}", formatAuditData(auditData));
        
        logger.debug("Logged SQL injection attempt: pattern={}, blocked={}", injectionPattern, blocked);
    }

    /**
     * Logs a cross-site scripting (XSS) attempt.
     * 
     * @param userId The user ID associated with the attempt (may be null)
     * @param xssPattern The detected XSS pattern
     * @param targetField The field that was targeted
     * @param ipAddress The IP address of the attacker
     * @param sanitized Whether the input was sanitized
     * 
     * Requirements: 7.5 - XSS attack detection
     */
    public void logXssAttempt(String userId, String xssPattern, String targetField, 
                             String ipAddress, boolean sanitized) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "xss_attempt");
        auditData.put("user_id", userId != null ? userId : "anonymous");
        auditData.put("xss_pattern", xssPattern);
        auditData.put("target_field", targetField);
        auditData.put("ip_address", ipAddress);
        auditData.put("sanitized", sanitized);
        auditData.put("severity", "high");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "xss_attempt");
            traceContextManager.addBusinessContext("security.xss.pattern", xssPattern);
            traceContextManager.addBusinessContext("security.sanitized", String.valueOf(sanitized));
            if (userId != null) {
                traceContextManager.addBusinessContext("security.user.id", userId);
            }
        }
        
        auditLogger.error("XSS attempt detected: {}", formatAuditData(auditData));
        
        logger.debug("Logged XSS attempt: pattern={}, sanitized={}", xssPattern, sanitized);
    }

    /**
     * Logs a suspicious activity pattern.
     * 
     * @param userId The user ID associated with the activity
     * @param activityType The type of suspicious activity (e.g., "unusual_location", "unusual_time", "rapid_requests")
     * @param activityDetails Details about the suspicious activity
     * @param riskScore The calculated risk score (0-100)
     * @param actionTaken The action taken in response
     * 
     * Requirements: 7.5 - Suspicious activity detection
     */
    public void logSuspiciousActivity(String userId, String activityType, String activityDetails, 
                                     int riskScore, String actionTaken) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "suspicious_activity");
        auditData.put("user_id", userId);
        auditData.put("activity_type", activityType);
        auditData.put("activity_details", activityDetails);
        auditData.put("risk_score", riskScore);
        auditData.put("action_taken", actionTaken);
        
        // Determine severity based on risk score
        String severity;
        if (riskScore >= 80) {
            severity = "critical";
        } else if (riskScore >= 60) {
            severity = "high";
        } else if (riskScore >= 40) {
            severity = "medium";
        } else {
            severity = "low";
        }
        auditData.put("severity", severity);
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "suspicious_activity");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.activity.type", activityType);
            traceContextManager.addBusinessContext("security.risk.score", String.valueOf(riskScore));
            traceContextManager.addBusinessContext("security.severity", severity);
        }
        
        // Use severity-based logging
        if (riskScore >= 80) {
            auditLogger.error("CRITICAL suspicious activity: {}", formatAuditData(auditData));
        } else if (riskScore >= 60) {
            auditLogger.error("HIGH suspicious activity: {}", formatAuditData(auditData));
        } else if (riskScore >= 40) {
            auditLogger.warn("MEDIUM suspicious activity: {}", formatAuditData(auditData));
        } else {
            auditLogger.info("Suspicious activity: {}", formatAuditData(auditData));
        }
        
        logger.debug("Logged suspicious activity for user: {}, type: {}, risk: {}", 
                    userId, activityType, riskScore);
    }

    /**
     * Logs a policy violation.
     * 
     * @param userId The user ID who violated the policy
     * @param policyName The name of the policy violated
     * @param policyRule The specific rule violated
     * @param violationDetails Details about the violation
     * @param actionTaken The action taken in response
     * 
     * Requirements: 7.5 - Policy violation detection
     */
    public void logPolicyViolation(String userId, String policyName, String policyRule, 
                                  String violationDetails, String actionTaken) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "policy_violation");
        auditData.put("user_id", userId);
        auditData.put("policy_name", policyName);
        auditData.put("policy_rule", policyRule);
        auditData.put("violation_details", violationDetails);
        auditData.put("action_taken", actionTaken);
        auditData.put("severity", "medium");
        auditData.put("timestamp", Instant.now().toString());
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "policy_violation");
            traceContextManager.addBusinessContext("security.user.id", userId);
            traceContextManager.addBusinessContext("security.policy.name", policyName);
            traceContextManager.addBusinessContext("security.policy.rule", policyRule);
        }
        
        auditLogger.warn("Policy violation: {}", formatAuditData(auditData));
        
        logger.debug("Logged policy violation for user: {}, policy: {}, rule: {}", 
                    userId, policyName, policyRule);
    }

    /**
     * Logs integration with existing error handling for security exceptions.
     * This method should be called from exception handlers to log security-related exceptions.
     * 
     * @param exception The security exception
     * @param userId The user ID associated with the exception (may be null)
     * @param context Additional context about the exception
     * 
     * Requirements: 7.5 - Integration with error handling for security exceptions
     */
    public void logSecurityException(Exception exception, String userId, String context) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event_type", "security_exception");
        auditData.put("user_id", userId != null ? userId : "anonymous");
        auditData.put("exception_type", exception.getClass().getSimpleName());
        auditData.put("exception_message", exception.getMessage());
        auditData.put("context", context);
        auditData.put("timestamp", Instant.now().toString());
        
        // Determine severity based on exception type
        String severity = determineExceptionSeverity(exception);
        auditData.put("severity", severity);
        
        // Add trace context for correlation
        addTraceContext(auditData);
        
        // Add business context to current span
        if (traceContextManager.hasActiveTrace()) {
            traceContextManager.addBusinessContext("security.event.type", "security_exception");
            traceContextManager.addBusinessContext("security.exception.type", exception.getClass().getSimpleName());
            traceContextManager.addBusinessContext("security.severity", severity);
            if (userId != null) {
                traceContextManager.addBusinessContext("security.user.id", userId);
            }
        }
        
        // Use severity-based logging
        if ("critical".equalsIgnoreCase(severity) || "high".equalsIgnoreCase(severity)) {
            auditLogger.error("Security exception: {}", formatAuditData(auditData), exception);
        } else {
            auditLogger.warn("Security exception: {}", formatAuditData(auditData), exception);
        }
        
        logger.debug("Logged security exception: type={}, severity={}", 
                    exception.getClass().getSimpleName(), severity);
    }

    /**
     * Determines the severity of a security exception.
     * 
     * @param exception The exception to analyze
     * @return The severity level
     */
    private String determineExceptionSeverity(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        
        if (exceptionName.contains("authentication") || exceptionName.contains("credential")) {
            return "high";
        } else if (exceptionName.contains("authorization") || exceptionName.contains("access")) {
            return "medium";
        } else if (exceptionName.contains("validation")) {
            return "low";
        } else if (exceptionName.contains("security")) {
            return "high";
        }
        
        return "medium";
    }

    // Helper methods

    /**
     * Adds trace context to audit data for correlation with distributed traces.
     * 
     * @param auditData The audit data map to enhance with trace context
     */
    private void addTraceContext(Map<String, Object> auditData) {
        if (traceContextManager.hasActiveTrace()) {
            String traceId = traceContextManager.getCurrentTraceId();
            String spanId = traceContextManager.getCurrentSpanId();
            
            if (traceId != null) {
                auditData.put("trace_id", traceId);
            }
            if (spanId != null) {
                auditData.put("span_id", spanId);
            }
        }
    }

    /**
     * Formats audit data as a structured string for logging.
     * 
     * @param auditData The audit data to format
     * @return Formatted audit data string
     */
    private String formatAuditData(Map<String, Object> auditData) {
        StringBuilder sb = new StringBuilder();
        auditData.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }
}
