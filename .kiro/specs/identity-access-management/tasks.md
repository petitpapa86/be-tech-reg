# Identity & Access Management Context - Implementation Plan

## Implementation Tasks

- [x] 1. Create core value objects with factory methods and validation
  - Implement UserId, Email, FullName, and Address as Java records
  - Add factory methods with Result<T, ErrorDetail> return types for validation
  - Create PasswordHash record with bcrypt hashing functionality
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 2. Implement User aggregate with pure business logic
  - Create User record with immutable fields and business methods
  - Add User.create() factory method with validation and auto-ADMIN role assignment
  - Implement assignToBank() method for bank role management
  - _Requirements: 1.1, 2.1, 2.2, 3.3_

- [x] 3. Build BankRoleAssignment aggregate for multi-bank access control
  - Implement BankRoleAssignment record with assignment lifecycle methods
  - Add factory method for creating assignments with validation
  - Create revoke() method for deactivating assignments
  - Implement canManageBank() and other permission checking methods
  - _Requirements: 3.1, 3.2, 3.3, 3.6_

- [x] 4. Create UserRole enum with hierarchical permissions
  - Implement UserRole enum with VIEWER, ANALYST, COMPLIANCE_OFFICER, BANK_ADMIN, SYSTEM_ADMIN
  - Add canAccess() method for role hierarchy checking
  - Create SubscriptionTier enum with bank limits and pricing
  - _Requirements: 3.1, 3.2, 8.2_

- [x] 5. Implement pure user registration function
  - Create UserRegistrationService with registerUser() pure function
  - Add closure-based dependencies for user lookup, save, and subscription creation
  - Implement email uniqueness validation and password hashing
  - Create RegistrationResponse with nextStep guidance for bank configuration
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 6. Build authentication service with pure functions
  - Implement AuthenticationService with authenticateUser() pure function
  - Add password verification using closure-based password checker
  - Create JWT token generation with closure-based token factory
  - Implement user status validation (active/inactive checking)
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 7. Create bank role assignment service with subscription limit validation
  - Implement BankRoleService with assignUserToBank() pure function
  - Add subscription tier limit validation before bank assignment
  - Create validation for user existence and bank existence
  - Implement automatic BANK_ADMIN assignment during bank configuration
  - _Requirements: 3.3, 8.2, 8.4, 8.5_

- [x] 8. Build TenantContext for Service Composer integration
  - Create TenantContext record with userId, email, bankAccess, and isSystemAdmin
  - Implement canAccessBank() and canManageBank() permission methods
  - Add getRoleForBank() and getAccessibleBanks() utility methods
  - Create BankAccess record with role-based permission checking
  - _Requirements: 3.4, 3.5, 5.1, 5.2_

- [x] 9. Implement AuthorizationReactor for Service Composer
  - Create AuthorizationReactor implementing both GET and POST handler interfaces
  - Add JWT token extraction and validation from Authorization header
  - Implement TenantContext creation and injection into composition model
  - Add error handling for missing or invalid tokens with proper model updates
  - _Requirements: 3.4, 3.5, 5.1, 5.2, 5.3_

- [x] 10. Create RegistrationReactor for user registration orchestration
  - Implement RegistrationReactor for POST /auth/register route
  - Add command extraction from request body with validation
  - Coordinate with billing context for subscription creation
  - Implement three-phase execution: initialization, billing confirmation, completion
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 11. Build JWT token management with session handling
  - Create JwtToken value object with factory methods and validation
  - Implement JWT generation with user claims and expiration
  - Add JWT validation with signature verification and expiry checking
  - Create UserSession aggregate for session lifecycle management
  - _Requirements: 4.2, 4.3, 4.4, 4.5_

- [x] 12. Implement repository interfaces with closure-based design
  - Create UserRepository interface with closure-based method signatures
  - Add BankRoleAssignmentRepository interface for role management
  - Implement UserSessionRepository interface for session storage
  - Create repository factory functions for dependency injection
  - _Requirements: 2.4, 2.5, 4.1, 4.2_

- [x] 13. Build password security with bcrypt integration
  - Implement PasswordHash record with bcrypt hashing and verification
  - Add password strength validation with configurable requirements
  - Create secure password comparison with timing attack protection
  - Implement password reset functionality with secure token generation
  - _Requirements: 2.3, 4.1, 4.2_

- [x] 14. Create audit logging and security event tracking
  - Implement SecurityEventLogger for authentication and authorization events
  - Add audit trail for user registration, login attempts, and role changes
  - Create suspicious activity detection with automatic account locking
  - Implement compliance reporting for audit logs and access patterns
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 15. Implement API security and rate limiting
  - Create rate limiting middleware with per-user and per-IP limits
  - Add API key authentication for service-to-service communication
  - Implement progressive delays and temporary blocks for suspicious patterns
  - Configure security headers (CSP, HSTS, X-Frame-Options, etc.) and CORS
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 16. Build authentication & external identity integration
  - Implement JWT-based authentication for email/password login
  - Integrate Google and Facebook login using OAuth2/OpenID Connect
  - Map external user attributes (email, profile info) to internal user records
  - Create session handling and token refresh logic
  - Implement automatic user provisioning on first social login
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 17. Create comprehensive error handling and validation
  - Implement IamErrorCodes constants for all domain-specific errors
  - Add IamErrorHandler for consistent error processing and logging
  - Aggregate validation errors for complex command validation
  - Implement security event logging (e.g., failed login attempts, suspicious activity)
  - _Requirements: 2.1, 2.2, 4.1, 6.1, 9.5_

- [x] 18. Implement configuration and Spring Boot integration
  - Create IAM configuration properties for JWT settings, OAuth2 providers, rate limits, and security
  - Add Spring Boot auto-configuration for IAM services and repositories
  - Implement conditional bean creation based on profiles (e.g., dev, prod)
  - Create health check endpoints for IAM service monitoring
  - _Requirements: 4.1, 4.2, 9.1, 9.2_

- [x] 19. Create REST endpoints and API documentation
  - Implement UserController with registration, authentication, and profile endpoints
  - Add RoleController for role and permission management
  - Create API documentation with OpenAPI/Swagger (including security schemas)
  - Implement proper HTTP status codes and consistent error responses
  - _Requirements: 1.1, 1.4, 3.1, 3.2, 8.1_

- [ ] 20. Create JPA entities and database integration
  - Implement UserEntity, UserRoleEntity, and UserSessionEntity
  - Add JPA repositories for domain persistence
  - Create database migration scripts for IAM schema (Flyway/Liquibase)
  - Implement entity-to-domain model mapping with validation
  - _Requirements: 2.4, 2.5, 4.1, 4.2_