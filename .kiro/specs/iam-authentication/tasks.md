# Implementation Plan - IAM Authentication

## Overview
This implementation plan covers the development of comprehensive authentication capabilities for the RegTech IAM module, including login, token refresh, bank selection, and logout functionality. The implementation follows Clean Architecture with DDD principles and OAuth 2.0 best practices.

## Tasks

- [x] 1. Set up domain layer for Bank entity
  - Create Bank aggregate with identity, name, and status management
  - Create BankId value object with UUID generation and validation
  - Create BankName value object with validation (2-200 characters)
  - Create BankStatus enum (ACTIVE, INACTIVE)
  - Create IBankRepository interface for bank persistence
  - Add business methods: activate(), deactivate(), updateName(), isActive()
  - _Requirements: 13.1, 13.2, 13.5_

- [x] 2. Set up domain layer for refresh tokens
  - Create RefreshToken aggregate with lifecycle management (creation, validation, revocation)
  - Create RefreshTokenId value object with UUID generation and validation
  - Create TokenPair value object to represent access token and refresh token pairs
  - Create IRefreshTokenRepository interface for refresh token persistence
  - Create domain events: RefreshTokenCreatedEvent, RefreshTokenRevokedEvent, UserLoggedInEvent, UserLoggedOutEvent
  - _Requirements: 2.1, 2.4, 6.1, 6.2, 9.1, 9.2, 9.3, 9.4_

- [x] 2. Implement password hashing infrastructure





  - Create PasswordHasher component in infrastructure layer using BCrypt with work factor 12
  - Update Password value object to use PasswordHasher for hashing and verification
  - Ensure constant-time comparison for password verification to prevent timing attacks
  - _Requirements: 5.3, 8.5, 12.1, 12.2, 12.3_

- [x] 3. Create database schema for refresh tokens





  - Create Flyway migration V{timestamp}__Create_refresh_tokens_table.sql
  - Add refresh_tokens table with columns: id, user_id, token_hash, expires_at, created_at, revoked, revoked_at
  - Add foreign key constraint to users table with CASCADE delete
  - Create indexes on user_id, token_hash, expires_at, and partial index on revoked
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 4. Implement refresh token infrastructure





  - Create RefreshTokenEntity JPA entity
  - Create SpringDataRefreshTokenRepository interface extending JpaRepository
  - Create JpaRefreshTokenRepository implementing IRefreshTokenRepository
  - Implement mapper between RefreshToken domain model and RefreshTokenEntity
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 5. Implement login command and handler






  - Create LoginCommand with validation for email and password
  - Create LoginCommandHandler in application layer
  - Implement user authentication with password verification
  - Implement token pair generation (access token + refresh token)
  - Handle single bank auto-selection, multiple bank selection requirement, and no bank scenarios
  - Add structured async logging for login attempts and results
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 5.4, 9.1, 9.2, 10.1, 10.2_

- [x] 6. Implement token refresh command and handler





  - Create RefreshTokenCommand with validation
  - Create RefreshTokenCommandHandler in application layer
  - Implement refresh token validation and lookup
  - Implement token rotation (revoke old token, issue new token pair)
  - Maintain user's current tenant context during refresh
  - Add structured async logging for token refresh events
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 9.3_

- [x] 7. Implement bank selection command and handler





  - Create SelectBankCommand with validation for userId, bankId, and refreshToken
  - Create SelectBankCommandHandler in application layer
  - Validate user has access to selected bank
  - Generate new token pair with selected bank's tenant context embedded
  - Add structured async logging for bank selection events
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 5.4, 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 8. Implement logout command and handler




  - Create LogoutCommand with validation
  - Create LogoutCommandHandler in application layer
  - Implement refresh token revocation for user
  - Add structured async logging for logout events
  - Ensure logout returns success even if revocation fails (fail-safe)
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 9.4_

- [x] 9. Create presentation layer for authentication endpoints





  - Create AuthenticationController with handlers for login, refresh, select-bank, and logout
  - Create AuthenticationRoutes configuration with RouterFunction definitions
  - Mark login, refresh, and select-bank as public endpoints (no authentication required)
  - Mark logout as protected endpoint (requires authentication)
  - Create request DTOs: LoginRequest, RefreshTokenRequest, SelectBankRequest, LogoutRequest
  - Create response DTOs: LoginResponse, RefreshTokenResponse, SelectBankResponse, LogoutResponse
  - Create supporting DTOs: BankAssignmentDto, TenantContextDto
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 10. Update SecurityFilter for JWT validation





  - Update SecurityFilter to extract and validate JWT tokens from Authorization header
  - Implement JWT token validation using JwtToken.validate()
  - Populate SecurityContext with user details from JWT claims
  - Handle expired, invalid, and missing tokens with appropriate 401 responses
  - Ensure public authentication endpoints bypass token validation
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 11. Update IAMProperties configuration




  - Add JWT configuration properties: access-token-expiration-minutes, refresh-token-expiration-days
  - Add token cleanup configuration: enabled, cron schedule, retention-days
  - Update public-paths to include /api/v1/auth/login, /api/v1/auth/refresh, /api/v1/auth/select-bank
  - Update application-iam.yml with new configuration values
  - _Requirements: 5.1, 5.2, 6.5_

- [x] 12. Implement token cleanup scheduler





  - Create RefreshTokenCleanupScheduler component in infrastructure layer
  - Implement scheduled job to delete expired tokens older than 30 days
  - Run cleanup daily at 2 AM using cron expression
  - Add structured logging for cleanup operations
  - _Requirements: 6.5_

- [x] 13. Add error handling and security responses





  - Implement generic error messages for authentication failures (no user enumeration)
  - Add error codes: INVALID_CREDENTIALS, ACCOUNT_DISABLED, JWT_EXPIRED, JWT_INVALID_SIGNATURE, INVALID_REFRESH_TOKEN, BANK_ACCESS_DENIED, USER_NOT_FOUND, TOKEN_ALREADY_REVOKED
  - Ensure all authentication errors return consistent JSON error responses
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14. Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 15. Write unit tests for domain layer
  - Test RefreshToken creation, validation, expiration, and revocation
  - Test TokenPair generation with various user scenarios
  - Test RefreshTokenId validation and generation
  - Test domain events are raised correctly
  - _Requirements: 2.1, 2.4, 6.1, 6.2_

- [ ]* 16. Write unit tests for application layer
  - Test LoginCommandHandler with valid credentials, invalid credentials, inactive users, single bank, multiple banks, no banks
  - Test RefreshTokenCommandHandler with valid token, expired token, revoked token, token rotation
  - Test SelectBankCommandHandler with valid bank, invalid bank, forbidden access
  - Test LogoutCommandHandler with successful logout and error scenarios
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 17. Write unit tests for infrastructure layer
  - Test PasswordHasher BCrypt hashing and verification
  - Test JpaRefreshTokenRepository CRUD operations
  - Test RefreshTokenCleanupScheduler cleanup logic
  - Test RefreshTokenEntity to RefreshToken mapping
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 12.1, 12.3_

- [ ]* 18. Write integration tests for authentication flows
  - Test complete login → refresh → logout flow
  - Test multi-bank selection flow
  - Test token expiration and refresh
  - Test concurrent token refresh (race conditions)
  - Test invalid credentials, expired tokens, revoked tokens, tampered tokens
  - Test missing authorization headers
  - Test token persistence and revocation in database
  - Test foreign key constraints and cascade deletes
  - _Requirements: 1.1, 2.1, 2.4, 4.1, 6.1, 6.2, 6.3, 6.4, 11.1, 11.2, 11.3, 11.4_

- [ ]* 19. Write security tests
  - Test password hashing with BCrypt work factor 12
  - Test constant-time password comparison
  - Test token rotation on refresh
  - Test JWT signature validation
  - Test token expiration enforcement
  - Test generic error messages (no user enumeration)
  - _Requirements: 5.3, 8.1, 8.5, 12.1, 12.3_

- [ ] 20. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
