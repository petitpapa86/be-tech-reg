# IAM Package Consolidation Design

## Overview

This design consolidates two existing IAM implementations into a unified system that combines the comprehensive OAuth2 functionality from the legacy `identity_access_management` package with the modern Service Composer Framework patterns from the `iam` package. The consolidated system will provide a complete identity and access management solution with proper observability, event-driven architecture, and billing integration.

## Architecture

### Package Structure

The consolidated IAM system will use the following package structure:

```
com.bcbs239.compliance.iam/
├── domain/
│   ├── model/           # Domain entities and value objects
│   ├── services/        # Domain services and interfaces
│   ├── repositories/    # Repository interfaces
│   ├── events/          # Domain events
│   └── reactors/        # Service Composer Framework reactors
├── infrastructure/
│   ├── persistence/     # JPA repositories and entities
│   ├── oauth2/          # OAuth2 providers and configuration
│   ├── security/        # Security configuration and handlers
│   ├── web/             # Controllers and web configuration
│   ├── health/          # Health indicators
│   └── metrics/         # Metrics collection
└── application/
    ├── services/        # Application services
    └── commands/        # Command handlers
```

### Integration Points

The consolidated IAM system integrates with:
- **Billing Module**: For subscription management and tenant provisioning
- **Core Module**: For cross-module events and shared configuration
- **Service Composer Framework**: For reactive event processing
- **External OAuth2 Providers**: Google, Facebook, and extensible for others

## Components and Interfaces

### Domain Model

#### Core Entities

**User Entity**
```java
public class User {
    private UserId id;
    private Email email;
    private FullName fullName;
    private Address address;
    private Set<UserRole> roles;
    private Set<BankRoleAssignment> bankAssignments;
    private PasswordHash passwordHash; // Optional for OAuth2-only users
    private TenantContext tenantContext;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
}
```

**Authentication Token Models**
```java
public class AuthTokens {
    private JwtToken accessToken;
    private JwtToken refreshToken;
    private Instant expiresAt;
}

public class UserSession {
    private SessionId id;
    private UserId userId;
    private AuthTokens tokens;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastAccessedAt;
}
```

#### Value Objects

- `UserId`: Unique user identifier
- `Email`: Validated email address
- `FullName`: User's full name with validation
- `Address`: User's physical address
- `PasswordHash`: Securely hashed password
- `JwtToken`: JWT token with validation
- `TenantContext`: Multi-tenant context information

### Service Interfaces

#### Domain Services

**AuthenticationService**
```java
public interface AuthenticationService {
    Result<AuthTokens> authenticateWithPassword(Email email, String password);
    Result<AuthTokens> authenticateWithOAuth2(OAuth2UserInfo userInfo, OAuth2Provider provider);
    Result<AuthTokens> refreshToken(JwtToken refreshToken);
    Result<Void> logout(UserId userId, SessionId sessionId);
    Result<Boolean> validateToken(JwtToken token);
}
```

**UserRegistrationService**
```java
public interface UserRegistrationService {
    Result<RegistrationResponse> registerUser(RegistrationCommand command);
    Result<User> registerOAuth2User(OAuth2UserInfo userInfo, OAuth2Provider provider);
    Result<Void> confirmEmail(UserId userId, String confirmationToken);
    Result<Void> resendConfirmation(Email email);
}
```

**OAuth2AuthenticationService**
```java
public interface OAuth2AuthenticationService {
    Result<OAuth2UserInfo> exchangeCodeForUserInfo(String code, OAuth2Provider provider);
    Result<AuthTokens> processOAuth2Login(OAuth2UserInfo userInfo, OAuth2Provider provider);
    Set<OAuth2Provider> getSupportedProviders();
}
```

#### Repository Interfaces

**UserRepository**
```java
public interface UserRepository {
    Result<User> findById(UserId id);
    Result<User> findByEmail(Email email);
    Result<User> save(User user);
    Result<List<User>> findByTenant(TenantContext tenant);
    Result<Boolean> existsByEmail(Email email);
}
```

**UserSessionRepository**
```java
public interface UserSessionRepository {
    Result<UserSession> save(UserSession session);
    Result<UserSession> findById(SessionId id);
    Result<List<UserSession>> findActiveByUserId(UserId userId);
    Result<Void> invalidateSession(SessionId id);
    Result<Void> invalidateAllUserSessions(UserId userId);
}
```

### Reactive Components

#### Service Composer Reactors

**RegistrationReactor**
```java
@Component
public class RegistrationReactor implements Reactor<UserRegisteredEvent> {
    
    @Override
    public Result<Void> react(UserRegisteredEvent event, CompositionContext context) {
        // Trigger billing integration
        // Send welcome email
        // Initialize user preferences
        // Emit cross-module events
    }
}
```

**AuthorizationReactor**
```java
@Component
public class AuthorizationReactor implements Reactor<UserAuthenticatedEvent> {
    
    @Override
    public Result<Void> react(UserAuthenticatedEvent event, CompositionContext context) {
        // Update last login timestamp
        // Log security events
        // Check for suspicious activity
        // Update session tracking
    }
}
```

**OAuth2LoginReactor**
```java
@Component
public class OAuth2LoginReactor implements Reactor<OAuth2AuthenticationEvent> {
    
    @Override
    public Result<Void> react(OAuth2AuthenticationEvent event, CompositionContext context) {
        // Process OAuth2 user data
        // Create or update user profile
        // Handle provider-specific logic
        // Emit authentication events
    }
}
```

### Infrastructure Components

#### OAuth2 Providers

**OAuth2IdentityProvider Interface**
```java
public interface OAuth2IdentityProvider {
    OAuth2Provider getProvider();
    Result<OAuth2TokenResponse> exchangeCodeForToken(String code);
    Result<OAuth2UserInfo> getUserInfo(String accessToken);
    String getAuthorizationUrl(String state);
}
```

**Concrete Implementations**
- `GoogleOAuth2Provider`: Google OAuth2 integration
- `FacebookOAuth2Provider`: Facebook OAuth2 integration
- Extensible for additional providers

#### Security Configuration

**OAuth2SecurityConfig**
```java
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        // Configure OAuth2 login
        // Set up JWT authentication
        // Define authorization rules
        // Configure CORS and CSRF
    }
}
```

#### Web Controllers

**OAuth2Controller**
```java
@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {
    
    @GetMapping("/login/{provider}")
    public ResponseEntity<?> initiateLogin(@PathVariable String provider);
    
    @PostMapping("/callback/{provider}")
    public ResponseEntity<?> handleCallback(@PathVariable String provider, @RequestBody OAuth2CallbackRequest request);
}
```

## Data Models

### Database Schema

The consolidated system will use the following main tables:

**users**
- id (UUID, Primary Key)
- email (VARCHAR, Unique)
- full_name (VARCHAR)
- password_hash (VARCHAR, Nullable)
- address_line1, address_line2, city, state, postal_code, country
- tenant_id (UUID, Foreign Key)
- status (ENUM)
- created_at, updated_at, last_login_at (TIMESTAMP)

**user_roles**
- user_id (UUID, Foreign Key)
- role (VARCHAR)
- Primary Key: (user_id, role)

**bank_role_assignments**
- id (UUID, Primary Key)
- user_id (UUID, Foreign Key)
- bank_id (UUID, Foreign Key)
- role (VARCHAR)
- assigned_at (TIMESTAMP)

**user_sessions**
- id (UUID, Primary Key)
- user_id (UUID, Foreign Key)
- access_token_hash (VARCHAR)
- refresh_token_hash (VARCHAR)
- ip_address (VARCHAR)
- user_agent (TEXT)
- expires_at (TIMESTAMP)
- created_at, last_accessed_at (TIMESTAMP)

**oauth2_user_mappings**
- id (UUID, Primary Key)
- user_id (UUID, Foreign Key)
- provider (VARCHAR)
- provider_user_id (VARCHAR)
- provider_email (VARCHAR)
- created_at (TIMESTAMP)
- Unique: (provider, provider_user_id)

## Error Handling

### Error Types

The system defines specific error types for different failure scenarios:

```java
public enum IamErrorType {
    AUTHENTICATION_FAILED,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,
    EMAIL_NOT_VERIFIED,
    OAUTH2_PROVIDER_ERROR,
    OAUTH2_CODE_INVALID,
    SESSION_EXPIRED,
    INSUFFICIENT_PERMISSIONS,
    REGISTRATION_FAILED,
    PASSWORD_RESET_FAILED
}
```

### Error Handling Strategy

1. **Domain Level**: Use `Result<T>` pattern for all operations
2. **Application Level**: Convert domain errors to appropriate HTTP responses
3. **Infrastructure Level**: Handle external service failures gracefully
4. **Security Level**: Log security-related errors without exposing sensitive information

## Testing Strategy

### Unit Testing

- **Domain Models**: Test validation logic and business rules
- **Domain Services**: Test authentication and authorization logic
- **Reactors**: Test event processing and side effects
- **Repositories**: Test data access patterns with in-memory implementations

### Integration Testing

- **OAuth2 Flow**: Test complete OAuth2 authentication flows with mock providers
- **Database Integration**: Test repository implementations with test database
- **Security Configuration**: Test endpoint security and JWT validation
- **Cross-Module Events**: Test event publishing and consumption

### End-to-End Testing

- **User Registration**: Complete user registration flow including email confirmation
- **OAuth2 Login**: Full OAuth2 login flow with Google and Facebook
- **Password Authentication**: Traditional username/password authentication
- **Session Management**: Session creation, validation, and invalidation
- **Multi-Tenant**: Tenant isolation and context switching

### Performance Testing

- **Authentication Throughput**: Test concurrent authentication requests
- **Token Validation**: Test JWT validation performance
- **Database Queries**: Test repository query performance
- **OAuth2 Provider Calls**: Test external API call performance

## Migration Strategy

### Phase 1: Infrastructure Setup
1. Create new consolidated package structure
2. Set up database migrations for unified schema
3. Configure build and dependency management

### Phase 2: Domain Model Migration
1. Migrate and harmonize domain models
2. Create unified repository interfaces
3. Implement basic domain services

### Phase 3: OAuth2 Integration
1. Migrate OAuth2 providers and configuration
2. Update security configuration
3. Migrate OAuth2 controllers and handlers

### Phase 4: Service Composer Integration
1. Implement reactors for authentication events
2. Set up cross-module event publishing
3. Add health indicators and metrics

### Phase 5: Legacy Code Removal
1. Update all references to use new package
2. Remove old `identity_access_management` package
3. Update documentation and configuration

### Phase 6: Testing and Validation
1. Run comprehensive test suite
2. Perform security audit
3. Validate performance benchmarks
4. Update deployment scripts