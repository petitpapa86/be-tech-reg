# IAM Package Consolidation Implementation Plan

- [x] 1. Set up consolidated package structure and core interfaces
  - Create new package structure under `com.bcbs239.compliance.iam`
  - Define core domain model interfaces and value objects
  - Set up Result pattern for error handling
  - _Requirements: 1.1, 1.3, 1.4_

- [x] 2. Migrate and harmonize domain models

- [x] 2.1 Create unified User domain model
  - Merge User models from both packages into single comprehensive entity
  - Include OAuth2 and password authentication support
  - Add tenant context and billing integration fields
  - Write unit tests for User domain model validation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 2.2 Implement authentication token models
  - Create AuthTokens and JwtToken value objects
  - Implement UserSession entity for session management
  - Add token validation and expiration logic
  - Write unit tests for token models
  - _Requirements: 5.2, 5.4_

- [x] 2.3 Create value objects for user data
  - Implement Email, FullName, Address value objects with validation
  - Create UserId, SessionId, and other identifier types
  - Add PasswordHash value object with secure hashing
  - Write unit tests for all value objects
  - _Requirements: 5.5_

- [x] 3. Implement unified repository layer

- [x] 3.1 Create repository interfaces
  - Define UserRepository interface with comprehensive CRUD operations
  - Create UserSessionRepository for session management
  - Define BankRoleAssignmentRepository for role management
  - Add repository factory pattern for dependency injection
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 3.2 Implement JPA entities and repositories
  - Create JPA entities for User, UserSession, BankRoleAssignment in infrastructure/persistence
  - Implement JPA repository implementations for all domain repositories
  - Create repository implementation classes that bridge domain and JPA repositories
  - Set up database migrations for consolidated schema
  - Write integration tests for repository layer
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 4. Migrate OAuth2 functionality

- [x] 4.1 Create OAuth2 provider interfaces and implementations
  - Migrate GoogleOAuth2Provider from legacy package to new infrastructure/oauth2 package
  - Migrate FacebookOAuth2Provider from legacy package to new infrastructure/oauth2 package
  - Update OAuth2IdentityProvider interface to match consolidated design
  - Write unit tests for migrated OAuth2 providers
  - _Requirements: 2.1, 2.2, 2.4_

- [x] 4.2 Complete OAuth2 authentication service and user mapping implementation
  - Complete OAuth2UserMapper implementation to handle user creation and linking
  - Integrate OAuth2AuthenticationService with JWT token generation
  - Update service implementations to use consolidated domain models and repositories
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 4.3 Migrate OAuth2 web controllers and handlers
  - Migrate OAuth2Controller from legacy package to infrastructure/web
  - Migrate OAuth2AuthenticationSuccessHandler to infrastructure/oauth2
  - Migrate OAuth2AuthenticationFailureHandler to infrastructure/oauth2
  - Migrate OAuth2UserServiceImpl to infrastructure/oauth2
  - Update all handlers to use consolidated domain services
  - _Requirements: 2.1, 2.2, 2.3, 7.1, 7.4_

- [x] 5. Implement core authentication services







- [x] 5.1 Complete AuthenticationService implementation






  - Complete AuthenticationServiceImpl implementation (currently incomplete/truncated)
  - Integrate OAuth2 authentication with password-based authentication
  - Update to use consolidated domain models and repositories
  - Add session management integration
  - _Requirements: 2.1, 2.2, 2.4, 8.3_

- [x] 5.2 Create UserRegistrationService implementation






  - Create concrete implementation of UserRegistrationService interface
  - Integrate with billing module for subscription setup
  - Add proper error handling and validation
  - Wire with consolidated domain models and repositories
  - _Requirements: 6.1, 6.2, 8.1, 8.2_

- [x] 5.2.1 Create missing registration command and event models





  - Create RegistrationCommand value object for user registration requests
  - Create missing event classes (EmailVerifiedEvent, ConfirmationResendEvent)
  - Add proper validation and serialization support
  - **Note**: Follow closure-based implementation patterns as documented in CLOSURES_README.md
  - _Requirements: 6.1, 8.1_

- [x] 5.3 Migrate password management services





  - Migrate PasswordResetService from legacy package to domain/services
  - Migrate UserSessionService from legacy package to domain/services  
  - Migrate BankRoleService from legacy package to domain/services
  - Update services to use consolidated domain models and repositories
  - Integrate with unified repository layer and Result pattern
  - _Requirements: 8.1, 8.2, 8.4_

- [x] 5.4 Complete OAuth2AuthenticationService implementation





  - Complete OAuth2AuthenticationService implementation (interface exists, needs concrete implementation)
  - Integrate with migrated OAuth2 providers
  - Add proper error handling and validation
  - Wire with OAuth2UserMapper for user creation and linking
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 5.5 Complete OAuth2UserMapper implementation





  - Complete OAuth2UserMapper implementation (interface exists, needs concrete implementation)
  - Handle user creation from OAuth2 provider data
  - Implement user linking logic for existing accounts
  - Add proper validation and error handling
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 5.6 Create missing service implementations








  - Create JwtTokenServiceImpl concrete implementation
  - Create OAuth2AuthenticationServiceImpl concrete implementation
  - Ensure all service interfaces have corresponding implementations
  - Wire implementations with proper dependency injection
  - _Requirements: 2.1, 2.2, 7.2, 7.3_

- [-] 6. Complete Service Composer Framework integration





- [x] 6.1 Complete authentication event reactors implementation



  - Complete RegistrationReactor implementation for user registration workflow
  - Complete AuthorizationReactor integration with authentication service
  - Migrate AuthenticationReactor from legacy package to domain/reactors
  - Migrate OAuth2LoginReactor from legacy package to domain/reactors
  - Update reactors to use consolidated domain models and Result pattern
  - _Requirements: 3.1, 3.2_

- [x] 6.2 Set up cross-module event publishing

















  - Implement event publishing in domain services using existing events
  - Configure cross-module event bus integration
  - _Requirements: 3.2, 3.4_

- [x] 6.3 Complete observability components integration





  - Add OAuth2 provider health checks to existing IamModuleHealthIndicator
  - Integrate OAuth2 metrics with existing IamMetricsCollector
  - _Requirements: 3.3_

- [ ] 7. Implement security configuration

- [ ] 7.1 Migrate and consolidate security configuration
  - Migrate OAuth2SecurityConfig from legacy package to infrastructure/security
  - Integrate with existing JWT configuration from core module
  - Set up unified security filter chain for both OAuth2 and JWT
  - Configure CORS and CSRF protection
  - _Requirements: 7.1, 7.2, 7.4_

- [ ] 7.2 Complete JWT token service implementation
  - Complete JwtTokenService implementation (interface exists, needs concrete implementation)
  - Update to use consolidated JwtToken domain model
  - Integrate with existing JWT configuration from core module
  - Ensure compatibility with OAuth2 authentication flows
  - _Requirements: 7.2, 7.3_

- [ ] 8. Create application services and controllers

- [ ] 8.1 Create application services layer
  - Create UserApplicationService in application/services for user management operations
  - Create AuthenticationApplicationService for authentication workflows
  - Create RegistrationApplicationService for user registration workflows
  - Implement command handlers for user operations
  - _Requirements: 2.1, 2.2, 6.1_

- [ ] 8.2 Implement user management controllers
  - Create UserController in infrastructure/web for user profile management
  - Create AuthenticationController in infrastructure/web for login/logout endpoints
  - Create RegistrationController in infrastructure/web for user registration
  - Update existing OAuth2Controller to use consolidated services
  - Integrate controllers with application services
  - _Requirements: 2.1, 2.2, 6.1_

- [ ] 8.2.1 Create BillingIntegrationService interface
  - Create BillingIntegrationService interface in domain/services
  - Define methods for tenant provisioning and subscription management
  - Add billing event handling method signatures
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 8.3 Create billing integration service implementation
  - Create concrete implementation of BillingIntegrationService interface
  - Implement tenant provisioning and subscription management
  - Add billing event handling for user lifecycle events
  - Integrate with existing billing module through cross-module events
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 9. Update configuration and dependencies

- [ ] 9.1 Create IAM auto-configuration
  - Create IamAutoConfiguration class in infrastructure package (note: one exists in core.identity, may need consolidation)
  - Configure component scanning for consolidated IAM package structure
  - Set up conditional beans for OAuth2 and JWT configuration
  - Update application properties for OAuth2 providers and JWT settings
  - _Requirements: 1.1, 1.3, 7.1_

- [ ] 9.2 Update database migrations
  - Create Flyway migrations for consolidated schema
  - Add indexes for performance optimization
  - Set up foreign key constraints and relationships
  - _Requirements: 4.1, 4.2, 4.3_

- [ ] 10. Migrate existing references and remove legacy code

- [ ] 10.1 Update import statements across codebase
  - Search and replace all references to old package names throughout the codebase
  - Update Spring configuration and component references in core module
  - Update any external module references to IAM components
  - Fix any compilation errors from package changes
  - Update test files that still reference legacy package
  - _Requirements: 1.1, 1.2_

- [ ] 10.0.1 Verify domain model consolidation
  - Ensure all domain models in legacy package are properly replaced by consolidated versions
  - Verify no duplicate domain models exist between packages
  - Update any remaining references to legacy domain models
  - _Requirements: 1.1, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 10.1.1 Migrate remaining legacy infrastructure components
  - Migrate OAuth2SecurityConfig from legacy package to infrastructure/security
  - Verify OAuth2AuthenticationSuccessHandler and OAuth2AuthenticationFailureHandler are properly integrated
  - Verify OAuth2Controller is properly integrated with consolidated services
  - Verify OAuth2UserServiceImpl is properly integrated
  - Update any remaining references to legacy components
  - _Requirements: 1.1, 1.2, 7.1_

- [ ] 10.2 Remove legacy identity_access_management package
  - Verify all functionality has been successfully migrated and tested
  - Remove old identity_access_management package directory and all files
  - Update documentation to reflect new consolidated package structure
  - Clean up any unused dependencies from pom.xml
  - Update README and API documentation
  - _Requirements: 1.1, 1.2_

- [ ] 11. Comprehensive testing and validation

- [ ] 10.3 Migrate and update test files
  - Migrate remaining test files from legacy package to consolidated structure
  - Update test imports and references to use consolidated package
  - Ensure all new implementations have corresponding unit tests
  - Update integration tests to use consolidated services
  - _Requirements: 1.1, 1.2_

- [ ] 11.1 Run integration test suite
  - Execute all OAuth2 authentication flow tests
  - Test password-based authentication flows
  - Validate session management and token handling
  - _Requirements: 2.1, 2.2, 2.3, 3.2_

- [ ] 11.2 Perform security validation
  - Run security audit on authentication flows
  - Test JWT token security and validation
  - Validate OAuth2 provider integration security
  - Test authorization and access control
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 11.3 Validate billing integration
  - Test user registration with billing setup
  - Validate tenant context and subscription management
  - Test billing event handling and coordination
  - Verify billing service integration points
  - _Requirements: 6.1, 6.2, 6.3, 6.4_