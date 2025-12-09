# Authentication Error Codes

This document defines all error codes used in the IAM authentication module, ensuring consistent error handling and security best practices.

## Security Principles

1. **No User Enumeration**: Authentication failures return generic error messages that don't reveal whether an email exists or if the password was incorrect (Requirement 8.1)
2. **Consistent Responses**: All authentication errors return consistent JSON error responses (Requirement 8.3)
3. **Appropriate HTTP Status Codes**: Errors use correct HTTP status codes (401 for authentication, 403 for authorization, 400 for validation)

## Error Codes

### Authentication Errors (HTTP 401)

#### INVALID_CREDENTIALS
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "Invalid email or password"
- **Usage**: Returned for both invalid email and invalid password to prevent user enumeration
- **Requirements**: 1.4, 8.1
- **Used In**: LoginCommandHandler

#### ACCOUNT_DISABLED
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "Account is disabled"
- **Usage**: Returned when user account is not active
- **Requirements**: 1.5, 8.1
- **Used In**: LoginCommandHandler

#### JWT_EXPIRED
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "JWT token has expired" / "Access token has expired"
- **Usage**: Returned when JWT token has expired
- **Requirements**: 5.1, 11.2
- **Used In**: JwtToken.validate(), SecurityFilter

#### JWT_INVALID_SIGNATURE
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "JWT token has invalid signature" / "Invalid token signature"
- **Usage**: Returned when JWT signature verification fails
- **Requirements**: 5.3, 11.3
- **Used In**: JwtToken.validate(), SecurityFilter

#### JWT_MALFORMED
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "JWT token is malformed" / "Malformed token"
- **Usage**: Returned when JWT token format is invalid
- **Requirements**: 11.3
- **Used In**: JwtToken.validate(), SecurityFilter

#### JWT_VALIDATION_FAILED
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "JWT token validation failed: {details}"
- **Usage**: Returned for other JWT validation failures
- **Requirements**: 11.3
- **Used In**: JwtToken.validate()

#### INVALID_REFRESH_TOKEN
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "Invalid or expired refresh token"
- **Usage**: Returned when refresh token is invalid, expired, or revoked (generic message for security)
- **Requirements**: 2.2, 2.3, 8.1
- **Used In**: RefreshTokenCommandHandler

#### MISSING_TOKEN
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "Authentication token is required"
- **Usage**: Returned when Authorization header is missing for protected endpoints
- **Requirements**: 11.3
- **Used In**: SecurityFilter

#### AUTHENTICATION_ERROR
- **Type**: AUTHENTICATION_ERROR
- **HTTP Status**: 401 Unauthorized
- **Message**: "Authentication failed"
- **Usage**: Generic authentication error for unexpected failures
- **Requirements**: 8.4
- **Used In**: SecurityFilter

### Authorization Errors (HTTP 403)

#### BANK_ACCESS_DENIED
- **Type**: AUTHENTICATION_ERROR (treated as authorization error)
- **HTTP Status**: 401 Unauthorized (currently) / 403 Forbidden (recommended)
- **Message**: "User does not have access to the selected bank"
- **Usage**: Returned when user attempts to select a bank they don't have access to
- **Requirements**: 3.4, 10.4
- **Used In**: SelectBankCommandHandler

### Business Rule Errors (HTTP 400)

#### TOKEN_ALREADY_REVOKED
- **Type**: BUSINESS_RULE_ERROR
- **HTTP Status**: 400 Bad Request
- **Message**: "Refresh token is already revoked"
- **Usage**: Returned when attempting to revoke an already revoked token
- **Requirements**: 6.2
- **Used In**: RefreshToken.revoke()

### Not Found Errors (HTTP 404)

#### USER_NOT_FOUND
- **Type**: NOT_FOUND_ERROR
- **HTTP Status**: 404 Not Found
- **Message**: "User not found"
- **Usage**: Returned when user lookup fails (internal use only, not exposed to login endpoint)
- **Requirements**: 8.1
- **Used In**: RefreshTokenCommandHandler, SelectBankCommandHandler

#### BANK_NOT_FOUND
- **Type**: NOT_FOUND_ERROR
- **HTTP Status**: 404 Not Found
- **Message**: "Bank not found"
- **Usage**: Returned when bank lookup fails
- **Requirements**: 3.4
- **Used In**: SelectBankCommandHandler

### Validation Errors (HTTP 400)

#### VALIDATION_ERROR
- **Type**: VALIDATION_ERROR
- **HTTP Status**: 400 Bad Request
- **Message**: "Validation failed"
- **Usage**: Returned for field-level validation errors with detailed field error information
- **Requirements**: 8.3
- **Used In**: All command constructors

## Error Response Format

All errors return a consistent JSON structure:

### Validation Errors
```json
{
  "success": false,
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Email is required",
      "messageKey": "login.email.required"
    }
  ]
}
```

### Authentication Errors
```json
{
  "success": false,
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}
```

### Business Rule Errors
```json
{
  "success": false,
  "error": "TOKEN_ALREADY_REVOKED",
  "message": "Refresh token is already revoked",
  "messageKey": "refresh_token.already_revoked"
}
```

## Security Considerations

### User Enumeration Prevention

The following error codes use generic messages to prevent user enumeration:

1. **INVALID_CREDENTIALS**: Used for both "user not found" and "invalid password" scenarios
2. **INVALID_REFRESH_TOKEN**: Used for "token not found", "token expired", and "token revoked" scenarios

### Timing Attack Prevention

- Password verification uses BCrypt's constant-time comparison (Requirement 8.5, 12.3)
- All authentication failures return responses in similar timeframes

### Error Logging

- Failed authentication attempts are logged with details for security monitoring (Requirement 9.2)
- Logs include: email, IP address, failure reason, timestamp
- Sensitive information (passwords, tokens) is never logged (Requirement 12.2)

## Implementation Checklist

- [x] INVALID_CREDENTIALS - Used in LoginCommandHandler
- [x] ACCOUNT_DISABLED - Used in LoginCommandHandler
- [x] JWT_EXPIRED - Used in JwtToken.validate() and SecurityFilter
- [x] JWT_INVALID_SIGNATURE - Used in JwtToken.validate() and SecurityFilter
- [x] INVALID_REFRESH_TOKEN - Used in RefreshTokenCommandHandler
- [x] BANK_ACCESS_DENIED - Used in SelectBankCommandHandler
- [x] USER_NOT_FOUND - Used in RefreshTokenCommandHandler, SelectBankCommandHandler
- [x] TOKEN_ALREADY_REVOKED - Used in RefreshToken.revoke()
- [x] BaseController updated to recognize all authentication error codes
- [x] SecurityFilter returns consistent JSON error responses
- [x] All handlers use generic error messages for security-sensitive failures

## References

- Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
- Design Document: Error Handling and Security section
- Implementation: BaseController, SecurityFilter, Authentication Handlers
