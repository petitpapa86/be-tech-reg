# Security Audit Logging Guide

## Overview

The Security Audit Logging system provides comprehensive audit logging for authentication, authorization, sensitive data access, and security violations. All audit logs automatically include trace IDs and span IDs for correlation with distributed traces via Spring Boot 4's Micrometer Tracing.

## Components

### SecurityAuditLogger

The `SecurityAuditLogger` service provides methods for logging security-relevant events:

- **Authentication Events**: Login attempts, logout, session expiration, password changes
- **Authorization Events**: Access grants/denials, permission checks
- **Administrative Actions**: User management, role changes, configuration updates
- **Sensitive Data Access**: PII access, financial data access, bulk exports
- **Security Violations**: Brute force attacks, injection attempts, policy violations

### Automatic Trace Context Integration

All audit logs automatically include:
- `trace_id`: The distributed trace ID from Spring Boot 4's Micrometer Tracing
- `span_id`: The current span ID
- `timestamp`: UTC timestamp
- Business context tags added to the current span

### Log Storage

Security audit logs are written to:
- **File**: `logs/security-audit.log` (rotated daily, 90-day retention)
- **Console**: For development and debugging
- **Format**: Structured JSON for easy parsing and analysis

## Usage Examples

### 1. Authentication Logging

#### Basic Authentication Attempt

```java
@Service
public class AuthenticationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public Result<LoginResponse> login(String userId, String password) {
        boolean success = authenticateUser(userId, password);
        
        // Log authentication attempt
        securityAuditLogger.logAuthentication(
            userId,
            "password",
            success,
            success ? "Authentication successful" : "Invalid credentials"
        );
        
        return success ? Result.success(loginResponse) : Result.failure(error);
    }
}
```

#### Authentication with Context (IP Address, User Agent)

```java
@Controller
public class AuthenticationController {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public ServerResponse loginHandler(ServerRequest request) {
        String userId = extractUserId(request);
        String ipAddress = request.remoteAddress()
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");
        String userAgent = request.headers().firstHeader("User-Agent").orElse("unknown");
        
        boolean success = performAuthentication(userId);
        
        // Log authentication with full context
        securityAuditLogger.logAuthenticationWithContext(
            userId,
            "password",
            success,
            ipAddress,
            userAgent,
            success ? "Login successful" : "Invalid credentials"
        );
        
        return createResponse(success);
    }
}
```

#### Logout Logging

```java
@Service
public class AuthenticationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void logout(String userId, Instant loginTime) {
        long sessionDuration = Duration.between(loginTime, Instant.now()).getSeconds();
        
        // Log logout event
        securityAuditLogger.logLogout(
            userId,
            sessionDuration,
            "user_initiated"
        );
    }
}
```

#### Password Change Logging

```java
@Service
public class UserManagementService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public Result<Void> changePassword(String userId, String oldPassword, String newPassword) {
        boolean success = updatePassword(userId, oldPassword, newPassword);
        
        // Log password change
        securityAuditLogger.logPasswordChange(
            userId,
            "user",
            success,
            success ? "Password changed successfully" : "Old password incorrect"
        );
        
        return success ? Result.success() : Result.failure(error);
    }
}
```

### 2. Authorization Logging

#### Basic Authorization Decision

```java
@Service
public class AuthorizationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public boolean checkAccess(String userId, String resource, String action) {
        boolean granted = performAuthorizationCheck(userId, resource, action);
        
        // Log authorization decision
        securityAuditLogger.logAuthorization(
            userId,
            resource,
            action,
            granted,
            granted ? "User has required permissions" : "Insufficient permissions"
        );
        
        return granted;
    }
}
```

#### Authorization with Role and Permission Details

```java
@Service
public class AuthorizationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public boolean checkAccessWithDetails(String userId, String resource, String action) {
        User user = getUserById(userId);
        String userRole = user.getRole();
        String requiredPermission = getRequiredPermission(resource, action);
        boolean granted = user.hasPermission(requiredPermission);
        
        // Log authorization with full details
        securityAuditLogger.logAuthorizationWithPermissions(
            userId,
            userRole,
            resource,
            action,
            requiredPermission,
            granted,
            granted ? "User has required permission" : "User lacks permission: " + requiredPermission
        );
        
        return granted;
    }
}
```

### 3. Administrative Action Logging

#### User Management Actions

```java
@Service
public class UserAdministrationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void createUser(String adminUserId, User newUser) {
        userRepository.save(newUser);
        
        // Log administrative action
        securityAuditLogger.logAdministrativeAction(
            adminUserId,
            "user_create",
            "user",
            newUser.getId(),
            "Created user: " + newUser.getEmail() + ", role: " + newUser.getRole()
        );
    }
    
    public void updateUserRole(String adminUserId, String targetUserId, String oldRole, String newRole) {
        updateRole(targetUserId, newRole);
        
        // Log administrative action with before/after values
        securityAuditLogger.logAdministrativeActionWithChanges(
            adminUserId,
            "role_update",
            "user",
            targetUserId,
            oldRole,
            newRole,
            "Role change requested by administrator"
        );
    }
}
```

#### Configuration Changes

```java
@Service
public class ConfigurationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void updateConfiguration(String adminUserId, String configKey, String newValue) {
        String oldValue = getConfigValue(configKey);
        setConfigValue(configKey, newValue);
        
        // Log configuration change
        securityAuditLogger.logConfigurationChange(
            adminUserId,
            configKey,
            oldValue,
            newValue,
            "Configuration updated via admin panel"
        );
    }
}
```

### 4. Sensitive Data Access Logging

#### PII Access

```java
@Service
public class UserDataService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public UserProfile getUserProfile(String requestingUserId, String targetUserId) {
        UserProfile profile = userRepository.findById(targetUserId);
        
        // Log PII access
        securityAuditLogger.logPiiAccess(
            requestingUserId,
            "profile",
            targetUserId,
            "read",
            "User profile view"
        );
        
        return profile;
    }
}
```

#### Financial Data Access

```java
@Service
public class BillingService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public AccountBalance getAccountBalance(String userId, String accountId) {
        AccountBalance balance = accountRepository.getBalance(accountId);
        
        // Log financial data access
        securityAuditLogger.logFinancialDataAccess(
            userId,
            "balance",
            accountId,
            "read",
            "Account balance inquiry"
        );
        
        return balance;
    }
}
```

#### Bulk Data Export

```java
@Service
public class DataExportService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public byte[] exportUserData(String userId, ExportRequest request) {
        List<User> users = userRepository.findAll();
        boolean containsSensitiveData = request.includesPii() || request.includesFinancialData();
        
        byte[] exportData = generateExport(users, request.getFormat());
        
        // Log bulk data export
        securityAuditLogger.logBulkDataExport(
            userId,
            request.getFormat(),
            users.size(),
            request.getDataTypes(),
            containsSensitiveData,
            request.getPurpose()
        );
        
        return exportData;
    }
}
```

#### Sensitive Data Access Pattern Detection

```java
@Service
public class DataAccessMonitoringService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void detectAccessPatterns(String userId) {
        // Analyze access patterns
        AccessPattern pattern = analyzeUserAccessPattern(userId);
        
        if (pattern.isSuspicious()) {
            // Log suspicious access pattern
            securityAuditLogger.logSensitiveDataAccessPattern(
                userId,
                pattern.getType(),
                pattern.getDataType(),
                pattern.getAccessCount(),
                pattern.getTimeWindow(),
                pattern.getRiskLevel()
            );
        }
    }
}
```

### 5. Security Violation Logging

#### Brute Force Attack Detection

```java
@Service
public class LoginAttemptService {
    
    private final SecurityAuditLogger securityAuditLogger;
    private final Map<String, Integer> attemptCache = new ConcurrentHashMap<>();
    
    public void recordFailedLogin(String userId, String ipAddress) {
        int attempts = attemptCache.merge(ipAddress, 1, Integer::sum);
        
        if (attempts >= 5) {
            // Log brute force attempt
            securityAuditLogger.logBruteForceAttempt(
                userId,
                attempts,
                "5 minutes",
                ipAddress,
                "ip_blocked"
            );
            
            blockIpAddress(ipAddress);
        }
    }
}
```

#### SQL Injection Detection

```java
@Component
public class SqlInjectionDetector {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public boolean detectSqlInjection(String userId, String input, String ipAddress) {
        String injectionPattern = detectInjectionPattern(input);
        
        if (injectionPattern != null) {
            // Log SQL injection attempt
            securityAuditLogger.logSqlInjectionAttempt(
                userId,
                injectionPattern,
                "user_input",
                ipAddress,
                true
            );
            
            return true;
        }
        
        return false;
    }
}
```

#### XSS Attack Detection

```java
@Component
public class XssDetector {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public String sanitizeInput(String userId, String input, String fieldName, String ipAddress) {
        String xssPattern = detectXssPattern(input);
        
        if (xssPattern != null) {
            // Log XSS attempt
            securityAuditLogger.logXssAttempt(
                userId,
                xssPattern,
                fieldName,
                ipAddress,
                true
            );
            
            return sanitize(input);
        }
        
        return input;
    }
}
```

#### Suspicious Activity Detection

```java
@Service
public class SecurityMonitoringService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void monitorUserActivity(String userId) {
        ActivityAnalysis analysis = analyzeUserActivity(userId);
        
        if (analysis.isSuspicious()) {
            int riskScore = calculateRiskScore(analysis);
            
            // Log suspicious activity
            securityAuditLogger.logSuspiciousActivity(
                userId,
                analysis.getActivityType(),
                analysis.getDetails(),
                riskScore,
                determineAction(riskScore)
            );
        }
    }
}
```

#### Policy Violation Detection

```java
@Service
public class PolicyEnforcementService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public void enforcePolicy(String userId, String action) {
        PolicyViolation violation = checkPolicyCompliance(userId, action);
        
        if (violation != null) {
            // Log policy violation
            securityAuditLogger.logPolicyViolation(
                userId,
                violation.getPolicyName(),
                violation.getRuleName(),
                violation.getDetails(),
                violation.getActionTaken()
            );
        }
    }
}
```

#### Security Exception Handling

```java
@ControllerAdvice
public class SecurityExceptionHandler {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityException(
        SecurityException ex,
        HttpServletRequest request
    ) {
        String userId = extractUserId(request);
        String context = "Request: " + request.getRequestURI();
        
        // Log security exception
        securityAuditLogger.logSecurityException(ex, userId, context);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Security violation detected"));
    }
}
```

## Integration with Existing Error Handling

The SecurityAuditLogger integrates seamlessly with existing error handling:

```java
@Service
public class AuthenticationService {
    
    private final SecurityAuditLogger securityAuditLogger;
    
    public Result<LoginResponse> authenticate(String userId, String password) {
        try {
            // Perform authentication
            LoginResponse response = performAuthentication(userId, password);
            
            // Log successful authentication
            securityAuditLogger.logAuthentication(
                userId,
                "password",
                true,
                "Authentication successful"
            );
            
            return Result.success(response);
            
        } catch (AuthenticationException ex) {
            // Log failed authentication
            securityAuditLogger.logAuthentication(
                userId,
                "password",
                false,
                "Authentication failed: " + ex.getMessage()
            );
            
            // Log security exception
            securityAuditLogger.logSecurityException(ex, userId, "Login attempt");
            
            return Result.failure(DomainError.authentication("Invalid credentials"));
        }
    }
}
```

## Trace Context Correlation

All audit logs automatically include trace context for correlation with distributed traces:

```json
{
  "timestamp": "2024-12-09T10:30:00.000Z",
  "severity": "WARN",
  "logger": "SECURITY_AUDIT",
  "message": "Authentication attempt: event_type=authentication, user_id=user123, ...",
  "trace_id": "4bf92f3577b34da6a3ce929d0e0e4736",
  "span_id": "00f067aa0ba902b7",
  "thread": "http-nio-8080-exec-1"
}
```

This allows you to:
1. Correlate security events with application traces
2. Track the full request flow that led to a security event
3. Analyze security events in the context of business operations

## Best Practices

### 1. Log All Security-Relevant Events

Always log:
- Authentication attempts (success and failure)
- Authorization decisions (grants and denials)
- Administrative actions
- Sensitive data access
- Security violations

### 2. Include Sufficient Context

Provide enough context to understand the event:
- User ID
- IP address (when available)
- Resource being accessed
- Action being performed
- Outcome (success/failure)

### 3. Use Appropriate Severity Levels

The SecurityAuditLogger automatically uses appropriate severity levels:
- `INFO`: Successful operations, normal activity
- `WARN`: Failed operations, policy violations, administrative actions
- `ERROR`: Security violations, high-risk activities, critical events

### 4. Protect Sensitive Data

Never log:
- Passwords or credentials
- Full credit card numbers
- Social security numbers
- Other PII in plain text

Instead, log:
- User IDs or identifiers
- Data types accessed
- Actions performed

### 5. Monitor and Alert

Set up monitoring and alerting for:
- High-severity security events
- Unusual access patterns
- Multiple failed authentication attempts
- Policy violations

## Compliance

The security audit logging system supports compliance with:
- **GDPR**: Audit trail for data access and processing
- **SOC 2**: Security monitoring and incident response
- **PCI DSS**: Access control and monitoring requirements
- **HIPAA**: Audit controls for protected health information

## Log Retention

Security audit logs are retained for:
- **File logs**: 90 days (configurable in logback-spring.xml)
- **Centralized logging**: As configured in your log aggregation system

For compliance requirements, consider:
- Archiving logs to long-term storage
- Implementing log integrity verification
- Restricting access to audit logs

## Troubleshooting

### Logs Not Appearing

1. Check that the `SECURITY_AUDIT` logger is configured in logback-spring.xml
2. Verify the log level is set to `INFO` or lower
3. Check file permissions for the logs directory

### Missing Trace Context

1. Ensure Spring Boot 4 observability is properly configured
2. Verify that the request is within an active trace context
3. Check that Micrometer Tracing is enabled

### Performance Impact

The SecurityAuditLogger is designed for minimal performance impact:
- Asynchronous logging via Logback
- Efficient JSON encoding
- No blocking operations

If you experience performance issues:
1. Review log volume and frequency
2. Consider sampling for high-volume events
3. Optimize log appender configuration

## Additional Resources

- [Spring Boot 4 Observability Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer Tracing Documentation](https://micrometer.io/docs/tracing)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)
