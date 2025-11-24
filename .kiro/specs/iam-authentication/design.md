# Design Document - IAM Authentication

## Overview

This design document outlines the implementation of comprehensive authentication capabilities for the RegTech IAM module, including login, token refresh, bank selection, and logout functionality. The design follows Clean Architecture with DDD principles, OAuth 2.0 best practices, and the platform's established patterns.

### Key Features

- **User Login**: Email/password authentication with JWT token generation
- **Token Refresh**: OAuth 2.0 style refresh tokens with rotation
- **Bank Selection**: Multi-tenant context selection for users with multiple bank assignments
- **Logout**: Secure session termination with token revocation
- **Token Management**: Database-backed refresh token storage with automatic cleanup

### Design Principles

1. **Clean Architecture**: Strict layer separation (Domain → Application → Infrastructure → Presentation)
2. **DDD**: Rich domain models, value objects, and domain events
3. **Security First**: BCrypt password hashing, token rotation, constant-time comparisons
4. **Fail-Fast**: Validation at boundaries, Result types for error handling
5. **Audit Trail**: Structured async logging for all authentication events
6. **Multi-Tenancy**: Support for users across multiple banks with role-based access

## Architecture

### Layer Organization

```
regtech-iam/
├── domain/                    # Pure business logic
│   ├── authentication/        # Authentication aggregates and value objects
│   │   ├── RefreshToken.java
│   │   ├── RefreshTokenId.java
│   │   ├── TokenPair.java
│   │   └── IRefreshTokenRepository.java
│   ├── banks/                 # Bank domain
│   │   ├── Bank.java
│   │   ├── BankId.java
│   │   ├── BankName.java
│   │   ├── BankStatus.java
│   │   └── IBankRepository.java
│   └── users/                 # Existing user domain
│       ├── User.java
│       ├── JwtToken.java (existing)
│       └── UserRepository.java (existing)
│
├── application/               # Use cases and workflows
│   └── authentication/        # Authentication capability
│       ├── LoginCommand.java
│       ├── LoginCommandHandler.java
│       ├── RefreshTokenCommand.java
│       ├── RefreshTokenCommandHandler.java
│       ├── SelectBankCommand.java
│       ├── SelectBankCommandHandler.java
│       ├── LogoutCommand.java
│       └── LogoutCommandHandler.java
│
├── infrastructure/            # Technical implementations
│   ├── database/
│   │   ├── entities/
│   │   │   ├── RefreshTokenEntity.java
│   │   │   └── BankEntity.java
│   │   └── repositories/
│   │       ├── JpaRefreshTokenRepository.java
│   │       └── JpaBankRepository.java
│   └── security/
│       └── PasswordHasher.java
│
└── presentation/              # API layer
    └── authentication/
        ├── AuthenticationController.java
        ├── AuthenticationRoutes.java
        ├── LoginRequest.java
        ├── LoginResponse.java
        ├── RefreshTokenRequest.java
        ├── SelectBankRequest.java
        └── LogoutRequest.java
```


## Domain Layer Design

### 1. Bank Aggregate

**Purpose**: Represents a financial institution in the system with identity and status management

```java
public class Bank extends Entity {
    private final BankId id;
    private BankName name;
    private BankStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    // Factory method
    public static Result<Bank> create(
        BankId id,
        String name
    ) {
        // Validate name
        Result<BankName> nameResult = BankName.create(name);
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.getError().get());
        }
        
        return Result.success(new Bank(
            id,
            nameResult.getValue().get(),
            BankStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        ));
    }
    
    // Business methods
    public Result<Void> activate() {
        if (this.status == BankStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of(
                "BANK_ALREADY_ACTIVE",
                ErrorType.BUSINESS,
                "Bank is already active",
                "bank.already_active"
            ));
        }
        
        this.status = BankStatus.ACTIVE;
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    public Result<Void> deactivate() {
        if (this.status == BankStatus.INACTIVE) {
            return Result.failure(ErrorDetail.of(
                "BANK_ALREADY_INACTIVE",
                ErrorType.BUSINESS,
                "Bank is already inactive",
                "bank.already_inactive"
            ));
        }
        
        this.status = BankStatus.INACTIVE;
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    public boolean isActive() {
        return this.status == BankStatus.ACTIVE;
    }
    
    public Result<Void> updateName(String newName) {
        Result<BankName> nameResult = BankName.create(newName);
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.getError().get());
        }
        
        this.name = nameResult.getValue().get();
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    // Getters
    public BankId getId() { return id; }
    public BankName getName() { return name; }
    public BankStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

### 2. Bank Value Objects

**BankId**:
```java
public record BankId(UUID value) {
    public static BankId generate() {
        return new BankId(UUID.randomUUID());
    }
    
    public static Result<BankId> from(String value) {
        try {
            return Result.success(new BankId(UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_BANK_ID",
                ErrorType.VALIDATION,
                "Invalid bank ID format",
                "bank.id.invalid"
            ));
        }
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
```

**BankName**:
```java
public record BankName(String value) {
    public static Result<BankName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.fieldError(
                "name",
                "REQUIRED",
                "Bank name is required",
                "bank.name.required"
            ));
        }
        
        String trimmed = value.trim();
        if (trimmed.length() < 2) {
            return Result.failure(ErrorDetail.fieldError(
                "name",
                "TOO_SHORT",
                "Bank name must be at least 2 characters",
                "bank.name.too_short"
            ));
        }
        
        if (trimmed.length() > 200) {
            return Result.failure(ErrorDetail.fieldError(
                "name",
                "TOO_LONG",
                "Bank name must not exceed 200 characters",
                "bank.name.too_long"
            ));
        }
        
        return Result.success(new BankName(trimmed));
    }
}
```

**BankStatus**:
```java
public enum BankStatus {
    ACTIVE,
    INACTIVE
}
```

### 3. IBankRepository Interface

```java
public interface IBankRepository {
    Result<BankId> save(Bank bank);
    Maybe<Bank> findById(BankId id);
    List<Bank> findAll();
    List<Bank> findByStatus(BankStatus status);
    boolean existsById(BankId id);
}
```

### 4. RefreshToken Aggregate

**Purpose**: Represents a refresh token with lifecycle management

```java
public class RefreshToken extends Entity {
    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;  // BCrypt hash of token value
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean revoked;
    private Maybe<Instant> revokedAt;
    
    // Factory method
    public static Result<RefreshToken> create(
        UserId userId,
        String tokenValue,
        Duration expiration
    ) {
        // Validation
        // Hash token value
        // Create refresh token
    }
    
    // Business methods
    public Result<Void> revoke() {
        if (this.revoked) {
            return Result.failure(ErrorDetail.of(
                "TOKEN_ALREADY_REVOKED",
                ErrorType.BUSINESS,
                "Refresh token is already revoked",
                "refresh_token.already_revoked"
            ));
        }
        
        this.revoked = true;
        this.revokedAt = Maybe.some(Instant.now());
        
        // Raise domain event
        this.addDomainEvent(new RefreshTokenRevokedEvent(
            this.id,
            this.userId,
            Instant.now()
        ));
        
        return Result.success(null);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
    
    public boolean isValid() {
        return !this.revoked && !this.isExpired();
    }
}
```

### 2. TokenPair Value Object

**Purpose**: Represents an access token and refresh token pair

```java
public record TokenPair(
    JwtToken accessToken,
    RefreshToken refreshToken
) {
    public static Result<TokenPair> create(
        User user,
        String jwtSecret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration
    ) {
        // Generate access token
        Result<JwtToken> accessTokenResult = JwtToken.generate(
            user,
            jwtSecret,
            accessTokenExpiration
        );
        
        if (accessTokenResult.isFailure()) {
            return Result.failure(accessTokenResult.getError().get());
        }
        
        // Generate refresh token
        Result<RefreshToken> refreshTokenResult = RefreshToken.create(
            user.getId(),
            generateSecureToken(),
            refreshTokenExpiration
        );
        
        if (refreshTokenResult.isFailure()) {
            return Result.failure(refreshTokenResult.getError().get());
        }
        
        return Result.success(new TokenPair(
            accessTokenResult.getValue().get(),
            refreshTokenResult.getValue().get()
        ));
    }
    
    private static String generateSecureToken() {
        // Generate cryptographically secure random token
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

### 3. RefreshTokenId Value Object

```java
public record RefreshTokenId(UUID value) {
    public static RefreshTokenId generate() {
        return new RefreshTokenId(UUID.randomUUID());
    }
    
    public static Result<RefreshTokenId> from(String value) {
        try {
            return Result.success(new RefreshTokenId(UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN_ID",
                ErrorType.VALIDATION,
                "Invalid refresh token ID format",
                "refresh_token.id.invalid"
            ));
        }
    }
}
```

### 4. IRefreshTokenRepository Interface

```java
public interface IRefreshTokenRepository {
    Result<RefreshTokenId> save(RefreshToken refreshToken);
    Maybe<RefreshToken> findById(RefreshTokenId id);
    Maybe<RefreshToken> findByTokenHash(String tokenHash);
    Result<Void> revokeAllForUser(UserId userId);
    Result<Void> deleteExpiredTokens(Instant olderThan);
}
```

### 5. Domain Events

```java
// User logged in successfully
public class UserLoggedInEvent extends DomainEvent {
    private final UserId userId;
    private final Email email;
    private final Instant loginTime;
    private final String ipAddress;
}

// Refresh token created
public class RefreshTokenCreatedEvent extends DomainEvent {
    private final RefreshTokenId tokenId;
    private final UserId userId;
    private final Instant expiresAt;
}

// Refresh token revoked
public class RefreshTokenRevokedEvent extends DomainEvent {
    private final RefreshTokenId tokenId;
    private final UserId userId;
    private final Instant revokedAt;
}

// User logged out
public class UserLoggedOutEvent extends DomainEvent {
    private final UserId userId;
    private final Instant logoutTime;
}
```


## Application Layer Design

### 1. Login Command and Handler

**LoginCommand**:
```java
public record LoginCommand(
    String email,
    String password,
    Maybe<String> ipAddress
) {
    public static Result<LoginCommand> create(
        String email,
        String password,
        String ipAddress
    ) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        // Validate email
        if (email == null || email.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "email",
                "REQUIRED",
                "Email is required",
                "login.email.required"
            ));
        }
        
        // Validate password
        if (password == null || password.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "password",
                "REQUIRED",
                "Password is required",
                "login.password.required"
            ));
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        
        return Result.success(new LoginCommand(
            email,
            password,
            ipAddress != null ? Maybe.some(ipAddress) : Maybe.none()
        ));
    }
}
```

**LoginCommandHandler**:
```java
@Component
@Slf4j
public class LoginCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final String jwtSecret;
    private final Duration accessTokenExpiration = Duration.ofMinutes(15);
    private final Duration refreshTokenExpiration = Duration.ofDays(7);
    
    @Transactional
    public Result<LoginResponse> handle(LoginCommand command) {
        // Log login attempt
        log.info("LOGIN_ATTEMPT - email: {}, ipAddress: {}", 
            command.email(), 
            command.ipAddress().orElse("unknown"));
        
        // 1. Find user by email
        Maybe<User> userMaybe = userRepository.emailLookup(
            Email.create(command.email()).getValue().get()
        );
        
        if (userMaybe.isEmpty()) {
            log.warn("LOGIN_FAILED_USER_NOT_FOUND - email: {}", command.email());
            return Result.failure(ErrorDetail.of(
                "INVALID_CREDENTIALS",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid email or password",
                "login.invalid_credentials"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 2. Verify password
        if (!passwordHasher.verify(command.password(), user.getPasswordHash())) {
            log.warn("LOGIN_FAILED_INVALID_PASSWORD - userId: {}, email: {}", 
                user.getId().getValue().toString(), 
                command.email());
            return Result.failure(ErrorDetail.of(
                "INVALID_CREDENTIALS",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid email or password",
                "login.invalid_credentials"
            ));
        }
        
        // 3. Check if user is active
        if (!user.isActive()) {
            log.warn("LOGIN_FAILED_USER_INACTIVE - userId: {}", 
                user.getId().getValue().toString());
            return Result.failure(ErrorDetail.of(
                "ACCOUNT_DISABLED",
                ErrorType.AUTHENTICATION_ERROR,
                "Account is disabled",
                "login.account_disabled"
            ));
        }
        
        // 4. Generate token pair
        Result<TokenPair> tokenPairResult = TokenPair.create(
            user,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration
        );
        
        if (tokenPairResult.isFailure()) {
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 5. Save refresh token
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 6. Determine bank selection requirements
        List<UserRole> bankAssignments = userRepository.userRolesFinder(user.getId());
        
        LoginResponse response;
        if (bankAssignments.isEmpty()) {
            response = LoginResponse.withNoBanks(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken()
            );
        } else if (bankAssignments.size() == 1) {
            UserRole assignment = bankAssignments.get(0);
            
            // Load bank entity
            Maybe<Bank> bankMaybe = bankRepository.findById(
                BankId.from(assignment.getBankId()).getValue().get()
            );
            
            if (bankMaybe.isEmpty() || !bankMaybe.getValue().isActive()) {
                return Result.failure(ErrorDetail.of(
                    "BANK_NOT_AVAILABLE",
                    ErrorType.BUSINESS,
                    "Bank is not available",
                    "login.bank_not_available"
                ));
            }
            
            Bank bank = bankMaybe.getValue();
            TenantContext tenantContext = TenantContext.create(
                bank.getId().toString(),
                bank.getName().value(),
                assignment.getRole()
            );
            response = LoginResponse.withSingleBank(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tenantContext
            );
        } else {
            // Load all banks for user assignments
            List<BankAssignmentDto> availableBanks = new ArrayList<>();
            for (UserRole assignment : bankAssignments) {
                Maybe<Bank> bankMaybe = bankRepository.findById(
                    BankId.from(assignment.getBankId()).getValue().get()
                );
                
                // Only include active banks
                if (bankMaybe.isPresent() && bankMaybe.getValue().isActive()) {
                    Bank bank = bankMaybe.getValue();
                    availableBanks.add(new BankAssignmentDto(
                        bank.getId().toString(),
                        bank.getName().value(),
                        assignment.getRole()
                    ));
                }
            }
            
            if (availableBanks.isEmpty()) {
                return Result.failure(ErrorDetail.of(
                    "NO_ACTIVE_BANKS",
                    ErrorType.BUSINESS,
                    "No active banks available for user",
                    "login.no_active_banks"
                ));
            }
            
            response = LoginResponse.withMultipleBanks(
                user,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                availableBanks
            );
        }
        
        // 7. Log successful login
        log.info("LOGIN_SUCCESS - userId: {}, email: {}, requiresBankSelection: {}, ipAddress: {}", 
            user.getId().getValue().toString(),
            user.getEmail().getValue(),
            response.requiresBankSelection(),
            command.ipAddress().orElse("unknown"));
        
        return Result.success(response);
    }
}
```

### 2. Refresh Token Command and Handler

**RefreshTokenCommand**:
```java
public record RefreshTokenCommand(
    String refreshToken
) {
    public static Result<RefreshTokenCommand> create(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.failure(ErrorDetail.fieldError(
                "refreshToken",
                "REQUIRED",
                "Refresh token is required",
                "refresh_token.required"
            ));
        }
        
        return Result.success(new RefreshTokenCommand(refreshToken));
    }
}
```

**RefreshTokenCommandHandler**:
```java
@Component
@Slf4j
public class RefreshTokenCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final String jwtSecret;
    private final Duration accessTokenExpiration = Duration.ofMinutes(15);
    private final Duration refreshTokenExpiration = Duration.ofDays(7);
    
    @Transactional
    public Result<RefreshTokenResponse> handle(RefreshTokenCommand command) {
        // 1. Hash the provided token
        String tokenHash = passwordHasher.hash(command.refreshToken());
        
        // 2. Find refresh token in database
        Maybe<RefreshToken> tokenMaybe = refreshTokenRepository.findByTokenHash(tokenHash);
        
        if (tokenMaybe.isEmpty()) {
            log.warn("REFRESH_TOKEN_NOT_FOUND - tokenHash: {}...", 
                tokenHash.substring(0, 10));
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid or expired refresh token",
                "refresh_token.invalid"
            ));
        }
        
        RefreshToken refreshToken = tokenMaybe.getValue();
        
        // 3. Validate token
        if (!refreshToken.isValid()) {
            log.warn("REFRESH_TOKEN_INVALID - tokenId: {}, userId: {}, revoked: {}, expired: {}", 
                refreshToken.getId().value().toString(),
                refreshToken.getUserId().getValue().toString(),
                refreshToken.isRevoked(),
                refreshToken.isExpired());
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN",
                ErrorType.AUTHENTICATION_ERROR,
                "Invalid or expired refresh token",
                "refresh_token.invalid"
            ));
        }
        
        // 4. Load user
        Maybe<User> userMaybe = userRepository.userLoader(refreshToken.getUserId());
        
        if (userMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "USER_NOT_FOUND",
                ErrorType.NOT_FOUND,
                "User not found",
                "user.not_found"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 5. Revoke old refresh token (token rotation)
        Result<Void> revokeResult = refreshToken.revoke();
        if (revokeResult.isFailure()) {
            return Result.failure(revokeResult.getError().get());
        }
        
        refreshTokenRepository.save(refreshToken);
        
        // 6. Generate new token pair
        Result<TokenPair> tokenPairResult = TokenPair.create(
            user,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration
        );
        
        if (tokenPairResult.isFailure()) {
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 7. Save new refresh token
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 8. Get current tenant context from old token
        // (Maintain user's current bank selection)
        List<UserRole> bankAssignments = userRepository.userRolesFinder(user.getId());
        Maybe<TenantContext> tenantContext = extractTenantContextFromToken(
            command.refreshToken(),
            bankAssignments
        );
        
        // 9. Log token refresh
        log.info("TOKEN_REFRESHED - userId: {}, oldTokenId: {}, newTokenId: {}", 
            user.getId().getValue().toString(),
            refreshToken.getId().value().toString(),
            tokenPair.refreshToken().getId().value().toString());
        
        return Result.success(new RefreshTokenResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tenantContext
        ));
    }
}
```


### 3. Select Bank Command and Handler

**SelectBankCommand**:
```java
public record SelectBankCommand(
    UserId userId,
    String bankId,
    String refreshToken
) {
    public static Result<SelectBankCommand> create(
        String userId,
        String bankId,
        String refreshToken
    ) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (userId == null || userId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "userId",
                "REQUIRED",
                "User ID is required",
                "select_bank.user_id.required"
            ));
        }
        
        if (bankId == null || bankId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "bankId",
                "REQUIRED",
                "Bank ID is required",
                "select_bank.bank_id.required"
            ));
        }
        
        if (refreshToken == null || refreshToken.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "refreshToken",
                "REQUIRED",
                "Refresh token is required",
                "select_bank.refresh_token.required"
            ));
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        
        Result<UserId> userIdResult = UserId.from(userId);
        if (userIdResult.isFailure()) {
            return Result.failure(userIdResult.getError().get());
        }
        
        return Result.success(new SelectBankCommand(
            userIdResult.getValue().get(),
            bankId,
            refreshToken
        ));
    }
}
```

**SelectBankCommandHandler**:
```java
@Component
@Slf4j
public class SelectBankCommandHandler {
    private final UserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final String jwtSecret;
    private final Duration accessTokenExpiration = Duration.ofMinutes(15);
    private final Duration refreshTokenExpiration = Duration.ofDays(7);
    
    @Transactional
    public Result<SelectBankResponse> handle(SelectBankCommand command) {
        // 1. Validate user has access to the selected bank
        List<UserRole> bankAssignments = userRepository.userRolesFinder(command.userId());
        
        Maybe<UserRole> selectedBankMaybe = bankAssignments.stream()
            .filter(assignment -> assignment.getBankId().equals(command.bankId()))
            .findFirst()
            .map(Maybe::some)
            .orElse(Maybe.none());
        
        if (selectedBankMaybe.isEmpty()) {
            log.warn("BANK_SELECTION_FORBIDDEN - userId: {}, bankId: {}", 
                command.userId().getValue().toString(),
                command.bankId());
            return Result.failure(ErrorDetail.of(
                "BANK_ACCESS_DENIED",
                ErrorType.FORBIDDEN,
                "User does not have access to the selected bank",
                "select_bank.access_denied"
            ));
        }
        
        UserRole selectedBankAssignment = selectedBankMaybe.getValue();
        
        // 2. Load bank entity
        Result<BankId> bankIdResult = BankId.from(command.bankId());
        if (bankIdResult.isFailure()) {
            return Result.failure(bankIdResult.getError().get());
        }
        
        Maybe<Bank> bankMaybe = bankRepository.findById(bankIdResult.getValue().get());
        if (bankMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "BANK_NOT_FOUND",
                ErrorType.NOT_FOUND,
                "Bank not found",
                "bank.not_found"
            ));
        }
        
        Bank bank = bankMaybe.getValue();
        
        // 3. Verify bank is active
        if (!bank.isActive()) {
            log.warn("BANK_SELECTION_INACTIVE - userId: {}, bankId: {}", 
                command.userId().getValue().toString(),
                command.bankId());
            return Result.failure(ErrorDetail.of(
                "BANK_INACTIVE",
                ErrorType.BUSINESS,
                "Selected bank is not active",
                "select_bank.bank_inactive"
            ));
        }
        
        // 4. Load user
        Maybe<User> userMaybe = userRepository.userLoader(command.userId());
        if (userMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "USER_NOT_FOUND",
                ErrorType.NOT_FOUND,
                "User not found",
                "user.not_found"
            ));
        }
        
        User user = userMaybe.getValue();
        
        // 5. Create tenant context with selected bank
        TenantContext tenantContext = TenantContext.create(
            bank.getId().toString(),
            bank.getName().value(),
            selectedBankAssignment.getRole()
        );
        
        // 4. Generate new token pair with tenant context
        Result<TokenPair> tokenPairResult = TokenPair.createWithTenantContext(
            user,
            tenantContext,
            jwtSecret,
            accessTokenExpiration,
            refreshTokenExpiration
        );
        
        if (tokenPairResult.isFailure()) {
            return Result.failure(tokenPairResult.getError().get());
        }
        
        TokenPair tokenPair = tokenPairResult.getValue().get();
        
        // 5. Save new refresh token
        Result<RefreshTokenId> saveResult = refreshTokenRepository.save(
            tokenPair.refreshToken()
        );
        
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 6. Log bank selection
        log.info("BANK_SELECTED - userId: {}, bankId: {}, role: {}", 
            command.userId().getValue().toString(),
            command.bankId(),
            selectedBank.getRole());
        
        return Result.success(new SelectBankResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tenantContext
        ));
    }
}
```

### 4. Logout Command and Handler

**LogoutCommand**:
```java
public record LogoutCommand(
    UserId userId,
    String refreshToken
) {
    public static Result<LogoutCommand> create(String userId, String refreshToken) {
        if (userId == null || userId.isBlank()) {
            return Result.failure(ErrorDetail.fieldError(
                "userId",
                "REQUIRED",
                "User ID is required",
                "logout.user_id.required"
            ));
        }
        
        Result<UserId> userIdResult = UserId.from(userId);
        if (userIdResult.isFailure()) {
            return Result.failure(userIdResult.getError().get());
        }
        
        return Result.success(new LogoutCommand(
            userIdResult.getValue().get(),
            refreshToken
        ));
    }
}
```

**LogoutCommandHandler**:
```java
@Component
@Slf4j
public class LogoutCommandHandler {
    private final IRefreshTokenRepository refreshTokenRepository;
    
    @Transactional
    public Result<LogoutResponse> handle(LogoutCommand command) {
        try {
            // 1. Revoke all refresh tokens for the user
            Result<Void> revokeResult = refreshTokenRepository.revokeAllForUser(
                command.userId()
            );
            
            if (revokeResult.isFailure()) {
                log.error("LOGOUT_REVOKE_FAILED - userId: {}, error: {}", 
                    command.userId().getValue().toString(),
                    revokeResult.getError().get().getMessage());
                // Continue with logout even if revocation fails
            }
            
            // 2. Log logout event
            log.info("USER_LOGGED_OUT - userId: {}, timestamp: {}", 
                command.userId().getValue().toString(),
                Instant.now().toString());
            
            return Result.success(new LogoutResponse(
                "Logged out successfully",
                "logout.success"
            ));
            
        } catch (Exception e) {
            // Log error but still return success to client
            log.error("LOGOUT_ERROR - userId: {}", 
                command.userId().getValue().toString(), e);
            
            return Result.success(new LogoutResponse(
                "Logged out successfully",
                "logout.success"
            ));
        }
    }
}
```


## Infrastructure Layer Design

### 1. BankEntity (JPA Entity)

```java
@Entity
@Table(name = "banks", schema = "iam")
public class BankEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankStatus status;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Getters and setters
}
```

### 2. JpaBankRepository

```java
@Repository
@Slf4j
public class JpaBankRepository implements IBankRepository {
    private final SpringDataBankRepository jpaRepository;
    private final BankMapper mapper;
    
    @Override
    public Result<BankId> save(Bank bank) {
        try {
            BankEntity entity = mapper.toEntity(bank);
            BankEntity saved = jpaRepository.save(entity);
            
            return Result.success(BankId.from(
                saved.getId().toString()
            ).getValue().get());
        } catch (Exception e) {
            log.error("BANK_SAVE_FAILED - bankId: {}", 
                bank.getId().value().toString(), e);
            return Result.failure(ErrorDetail.of(
                "SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save bank",
                "bank.save.failed"
            ));
        }
    }
    
    @Override
    public Maybe<Bank> findById(BankId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain)
            .map(Maybe::some)
            .orElse(Maybe.none());
    }
    
    @Override
    public List<Bank> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Bank> findByStatus(BankStatus status) {
        return jpaRepository.findByStatus(status).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public boolean existsById(BankId id) {
        return jpaRepository.existsById(id.value());
    }
}

interface SpringDataBankRepository extends JpaRepository<BankEntity, UUID> {
    List<BankEntity> findByStatus(BankStatus status);
}
```

### 3. RefreshTokenEntity (JPA Entity)

```java
@Entity
@Table(name = "refresh_tokens", schema = "iam")
public class RefreshTokenEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    // Getters and setters
}
```

### 2. JpaRefreshTokenRepository

```java
@Repository
@Slf4j
public class JpaRefreshTokenRepository implements IRefreshTokenRepository {
    private final SpringDataRefreshTokenRepository jpaRepository;
    private final RefreshTokenMapper mapper;
    
    @Override
    public Result<RefreshTokenId> save(RefreshToken refreshToken) {
        try {
            RefreshTokenEntity entity = mapper.toEntity(refreshToken);
            RefreshTokenEntity saved = jpaRepository.save(entity);
            
            return Result.success(RefreshTokenId.from(
                saved.getId().toString()
            ).getValue().get());
        } catch (Exception e) {
            log.error("REFRESH_TOKEN_SAVE_FAILED - tokenId: {}", 
                refreshToken.getId().value().toString(), e);
            return Result.failure(ErrorDetail.of(
                "SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save refresh token",
                "refresh_token.save.failed"
            ));
        }
    }
    
    @Override
    public Maybe<RefreshToken> findById(RefreshTokenId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain)
            .map(Maybe::some)
            .orElse(Maybe.none());
    }
    
    @Override
    public Maybe<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
            .map(mapper::toDomain)
            .map(Maybe::some)
            .orElse(Maybe.none());
    }
    
    @Override
    public Result<Void> revokeAllForUser(UserId userId) {
        try {
            List<RefreshTokenEntity> tokens = jpaRepository.findByUserId(
                userId.getValue()
            );
            
            tokens.forEach(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
            });
            
            jpaRepository.saveAll(tokens);
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("REVOKE_ALL_TOKENS_FAILED - userId: {}", 
                userId.getValue().toString(), e);
            return Result.failure(ErrorDetail.of(
                "REVOKE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to revoke tokens",
                "refresh_token.revoke.failed"
            ));
        }
    }
    
    @Override
    public Result<Void> deleteExpiredTokens(Instant olderThan) {
        try {
            jpaRepository.deleteByExpiresAtBefore(olderThan);
            return Result.success(null);
        } catch (Exception e) {
            log.error("DELETE_EXPIRED_TOKENS_FAILED - olderThan: {}", 
                olderThan.toString(), e);
            return Result.failure(ErrorDetail.of(
                "DELETE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to delete expired tokens",
                "refresh_token.delete.failed"
            ));
        }
    }
}

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenEntity> findByUserId(UUID userId);
    void deleteByExpiresAtBefore(Instant expiresAt);
}
```

### 3. PasswordHasher

```java
@Component
public class PasswordHasher {
    private static final int BCRYPT_WORK_FACTOR = 12;
    
    /**
     * Hashes a password using BCrypt
     */
    public String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }
    
    /**
     * Verifies a password against a hash using constant-time comparison
     */
    public boolean verify(String plaintext, String hash) {
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (Exception e) {
            // Invalid hash format
            return false;
        }
    }
}
```

### 4. Database Migrations

**V{timestamp}__Create_banks_table.sql**:
```sql
-- Create banks table
CREATE TABLE IF NOT EXISTS iam.banks (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_bank_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Create indexes
CREATE INDEX idx_banks_status ON iam.banks(status);
CREATE INDEX idx_banks_name ON iam.banks(name);

-- Add comment
COMMENT ON TABLE iam.banks IS 'Stores bank entities for multi-tenant operations';
```

**V{timestamp}__Create_refresh_tokens_table.sql**:
```sql
-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS iam.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    
    CONSTRAINT fk_refresh_tokens_user 
        FOREIGN KEY (user_id) 
        REFERENCES iam.users(id) 
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_refresh_tokens_user_id ON iam.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON iam.refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON iam.refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON iam.refresh_tokens(revoked) WHERE revoked = FALSE;

-- Add comment
COMMENT ON TABLE iam.refresh_tokens IS 'Stores refresh tokens for OAuth 2.0 token refresh flow';
```

### 5. Token Cleanup Scheduler

```java
@Component
@Slf4j
public class RefreshTokenCleanupScheduler {
    private final IRefreshTokenRepository refreshTokenRepository;
    
    @Scheduled(cron = "0 0 2 * * *")  // Run daily at 2 AM
    public void cleanupExpiredTokens() {
        log.info("TOKEN_CLEANUP_STARTED - timestamp: {}", Instant.now().toString());
        
        // Delete tokens expired more than 30 days ago
        Instant cutoffDate = Instant.now().minus(Duration.ofDays(30));
        
        Result<Void> result = refreshTokenRepository.deleteExpiredTokens(cutoffDate);
        
        if (result.isSuccess()) {
            log.info("TOKEN_CLEANUP_COMPLETED - cutoffDate: {}", cutoffDate.toString());
        } else {
            log.error("TOKEN_CLEANUP_FAILED - cutoffDate: {}, error: {}", 
                cutoffDate.toString(),
                result.getError().get().getMessage());
        }
    }
}
```


## Presentation Layer Design

### 1. AuthenticationController and Routes

**AuthenticationRoutes**:
```java
@Configuration
public class AuthenticationRoutes {
    private final AuthenticationController controller;
    
    @Bean
    public RouterFunction<ServerResponse> authenticationRoutes() {
        // All authentication endpoints are public (no authentication required)
        RouterFunction<ServerResponse> publicRoutes = RouterFunctions
            .route(POST("/api/v1/auth/login").and(accept(APPLICATION_JSON)),
                controller::loginHandler)
            .andRoute(POST("/api/v1/auth/refresh").and(accept(APPLICATION_JSON)),
                controller::refreshTokenHandler)
            .andRoute(POST("/api/v1/auth/select-bank").and(accept(APPLICATION_JSON)),
                controller::selectBankHandler);
        
        // Logout requires authentication
        RouterFunction<ServerResponse> protectedRoutes = RouterFunctions
            .route(POST("/api/v1/auth/logout").and(accept(APPLICATION_JSON)),
                controller::logoutHandler);
        
        return RouterAttributes.asPublic(publicRoutes)
            .and(RouterAttributes.withAttributes(protectedRoutes, null, 
                List.of(Tags.AUTHENTICATION), "User logout"));
    }
}
```

**AuthenticationController**:
```java
@Configuration
public class AuthenticationController extends BaseController {
    private final LoginCommandHandler loginHandler;
    private final RefreshTokenCommandHandler refreshTokenHandler;
    private final SelectBankCommandHandler selectBankHandler;
    private final LogoutCommandHandler logoutHandler;
    
    public ServerResponse loginHandler(ServerRequest request) {
        // 1. Parse request
        LoginRequest req = parseRequest(request, LoginRequest.class);
        
        // 2. Extract IP address
        String ipAddress = request.remoteAddress()
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");
        
        // 3. Create command
        Result<LoginCommand> commandResult = LoginCommand.create(
            req.email(),
            req.password(),
            ipAddress
        );
        
        // 4. Handle validation errors
        if (commandResult.isFailure()) {
            return handleValidationErrors(commandResult.errors());
        }
        
        // 5. Execute command
        Result<LoginResponse> result = loginHandler.handle(
            commandResult.getValue().get()
        );
        
        // 6. Handle result
        return handleResult(
            result,
            "Login successful",
            "login.success"
        );
    }
    
    public ServerResponse refreshTokenHandler(ServerRequest request) {
        // 1. Parse request
        RefreshTokenRequest req = parseRequest(request, RefreshTokenRequest.class);
        
        // 2. Create command
        Result<RefreshTokenCommand> commandResult = RefreshTokenCommand.create(
            req.refreshToken()
        );
        
        // 3. Handle validation errors
        if (commandResult.isFailure()) {
            return handleValidationErrors(commandResult.errors());
        }
        
        // 4. Execute command
        Result<RefreshTokenResponse> result = refreshTokenHandler.handle(
            commandResult.getValue().get()
        );
        
        // 5. Handle result
        return handleResult(
            result,
            "Token refreshed successfully",
            "refresh_token.success"
        );
    }
    
    public ServerResponse selectBankHandler(ServerRequest request) {
        // 1. Parse request
        SelectBankRequest req = parseRequest(request, SelectBankRequest.class);
        
        // 2. Create command
        Result<SelectBankCommand> commandResult = SelectBankCommand.create(
            req.userId(),
            req.bankId(),
            req.refreshToken()
        );
        
        // 3. Handle validation errors
        if (commandResult.isFailure()) {
            return handleValidationErrors(commandResult.errors());
        }
        
        // 4. Execute command
        Result<SelectBankResponse> result = selectBankHandler.handle(
            commandResult.getValue().get()
        );
        
        // 5. Handle result
        return handleResult(
            result,
            "Bank selected successfully",
            "select_bank.success"
        );
    }
    
    public ServerResponse logoutHandler(ServerRequest request) {
        // 1. Get user ID from security context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String userId = securityContext.getUserId();
        
        // 2. Parse request
        LogoutRequest req = parseRequest(request, LogoutRequest.class);
        
        // 3. Create command
        Result<LogoutCommand> commandResult = LogoutCommand.create(
            userId,
            req.refreshToken()
        );
        
        // 4. Handle validation errors
        if (commandResult.isFailure()) {
            return handleValidationErrors(commandResult.errors());
        }
        
        // 5. Execute command
        Result<LogoutResponse> result = logoutHandler.handle(
            commandResult.getValue().get()
        );
        
        // 6. Handle result
        return handleResult(
            result,
            "Logged out successfully",
            "logout.success"
        );
    }
}
```

### 2. Request/Response DTOs

**LoginRequest**:
```java
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password
) {}
```

**LoginResponse**:
```java
public record LoginResponse(
    String userId,
    String email,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    boolean requiresBankSelection,
    List<BankAssignmentDto> availableBanks,
    Maybe<TenantContextDto> tenantContext,
    String nextStep
) {
    public static LoginResponse withSingleBank(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        TenantContext tenantContext
    ) {
        return new LoginResponse(
            user.getId().getValue().toString(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            Maybe.some(TenantContextDto.from(tenantContext)),
            "DASHBOARD"
        );
    }
    
    public static LoginResponse withMultipleBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        List<BankAssignmentDto> availableBanks
    ) {
        return new LoginResponse(
            user.getId().getValue().toString(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            true,
            availableBanks,
            Maybe.none(),
            "SELECT_BANK"
        );
    }
    
    public static LoginResponse withNoBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken
    ) {
        return new LoginResponse(
            user.getId().getValue().toString(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            Maybe.none(),
            "CONFIGURE_BANK"
        );
    }
}
```

**RefreshTokenRequest**:
```java
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
```

**RefreshTokenResponse**:
```java
public record RefreshTokenResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    Maybe<TenantContextDto> tenantContext
) {}
```

**SelectBankRequest**:
```java
public record SelectBankRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Bank ID is required")
    String bankId,
    
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
```

**SelectBankResponse**:
```java
public record SelectBankResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    TenantContextDto tenantContext
) {}
```

**LogoutRequest**:
```java
public record LogoutRequest(
    String refreshToken  // Optional - can revoke specific token or all tokens
) {}
```

**LogoutResponse**:
```java
public record LogoutResponse(
    String message,
    String messageKey
) {}
```

**Supporting DTOs**:
```java
public record BankAssignmentDto(
    String bankId,
    String bankName,
    String role
) {}

public record TenantContextDto(
    String bankId,
    String bankName,
    String role,
    List<String> permissions
) {
    public static TenantContextDto from(TenantContext context) {
        return new TenantContextDto(
            context.getBankId(),
            context.getBankName(),
            context.getRole(),
            context.getPermissions()
        );
    }
}
```


## Security Integration

### 1. SecurityFilter Updates

The existing `SecurityFilter` needs to be updated to validate JWT tokens and populate the `SecurityContext`:

```java
@Component
@Slf4j
public class SecurityFilter implements WebFilter {
    private final String jwtSecret;
    private final List<String> publicPaths;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 1. Check if path is public
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        // 2. Extract JWT token from Authorization header
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid authorization header");
        }
        
        String token = authHeader.substring(7);
        
        // 3. Validate JWT token
        Result<JwtToken.JwtClaims> claimsResult = JwtToken.validate(token, jwtSecret);
        
        if (claimsResult.isFailure()) {
            log.warn("JWT_VALIDATION_FAILED - path: {}, error: {}", 
                path,
                claimsResult.getError().get().getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
        
        JwtToken.JwtClaims claims = claimsResult.getValue().get();
        
        // 4. Populate SecurityContext
        SecurityContext securityContext = SecurityContext.create(
            claims.getUserId(),
            claims.getEmail(),
            extractTenantContext(claims)
        );
        
        SecurityContextHolder.setContext(securityContext);
        
        // 5. Continue filter chain
        return chain.filter(exchange)
            .doFinally(signalType -> SecurityContextHolder.clearContext());
    }
    
    private boolean isPublicPath(String path) {
        return publicPaths.stream()
            .anyMatch(publicPath -> pathMatches(path, publicPath));
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
```

### 2. Public Paths Configuration

Update `application.yml` to include authentication endpoints as public:

```yaml
iam:
  security:
    public-paths:
      # Authentication endpoints (no authentication required)
      - /api/v1/auth/login
      - /api/v1/auth/refresh
      - /api/v1/auth/select-bank
      
      # Health endpoints
      - /api/v1/ingestion/health
      - /api/v1/data-quality/health
      - /api/v1/data-quality/health/**
      - /api/v1/risk-calculation/health
      - /api/v1/risk-calculation/health/**
      - /api/v1/report-generation/health
      - /api/v1/report-generation/health/**
    
    jwt:
      secret: ${JWT_SECRET:changeme-in-production}
      access-token-expiration-minutes: 15
      refresh-token-expiration-days: 7
```

## Data Models

### Database Schema

**refresh_tokens table**:
```
Column         | Type         | Nullable | Default           | Description
---------------|--------------|----------|-------------------|---------------------------
id             | UUID         | NOT NULL | -                 | Primary key
user_id        | UUID         | NOT NULL | -                 | Foreign key to users table
token_hash     | VARCHAR(255) | NOT NULL | -                 | BCrypt hash of token value
expires_at     | TIMESTAMP    | NOT NULL | -                 | Token expiration timestamp
created_at     | TIMESTAMP    | NOT NULL | CURRENT_TIMESTAMP | Token creation timestamp
revoked        | BOOLEAN      | NOT NULL | FALSE             | Whether token is revoked
revoked_at     | TIMESTAMP    | NULL     | -                 | Revocation timestamp
```

**Indexes**:
- Primary key on `id`
- Index on `user_id` for user lookup
- Unique index on `token_hash` for token validation
- Index on `expires_at` for cleanup queries
- Partial index on `revoked` WHERE `revoked = FALSE` for active token queries

## Error Handling

### Error Codes and Messages

| Error Code | HTTP Status | Message | When |
|------------|-------------|---------|------|
| INVALID_CREDENTIALS | 401 | Invalid email or password | Login with wrong credentials |
| ACCOUNT_DISABLED | 401 | Account is disabled | Login with inactive account |
| JWT_EXPIRED | 401 | JWT token has expired | Access token expired |
| JWT_INVALID_SIGNATURE | 401 | JWT token has invalid signature | Tampered token |
| INVALID_REFRESH_TOKEN | 401 | Invalid or expired refresh token | Refresh with invalid token |
| BANK_ACCESS_DENIED | 403 | User does not have access to the selected bank | Select bank without access |
| USER_NOT_FOUND | 404 | User not found | User deleted after login |
| TOKEN_ALREADY_REVOKED | 400 | Refresh token is already revoked | Revoke already revoked token |

### Security Error Responses

All authentication errors return generic messages to prevent user enumeration:

```json
{
  "success": false,
  "message": "Invalid email or password",
  "messageKey": "login.invalid_credentials",
  "errors": []
}
```

Never reveal whether the email exists or if the password was incorrect.

## Testing Strategy

### Unit Tests

1. **Domain Layer**:
   - RefreshToken creation and validation
   - Token expiration logic
   - Token revocation
   - TokenPair generation

2. **Application Layer**:
   - LoginCommandHandler with various scenarios
   - RefreshTokenCommandHandler with token rotation
   - SelectBankCommandHandler with permission checks
   - LogoutCommandHandler

3. **Infrastructure Layer**:
   - PasswordHasher BCrypt operations
   - RefreshTokenRepository CRUD operations
   - Token cleanup scheduler

### Integration Tests

1. **Authentication Flow**:
   - Complete login → refresh → logout flow
   - Multi-bank selection flow
   - Token expiration and refresh
   - Concurrent token refresh (race conditions)

2. **Security Tests**:
   - Invalid credentials
   - Expired tokens
   - Revoked tokens
   - Tampered tokens
   - Missing authorization headers

3. **Database Tests**:
   - Token persistence
   - Token revocation
   - Cleanup of expired tokens
   - Foreign key constraints

## Performance Considerations

1. **Token Validation**: JWT validation is stateless and fast (no database lookup)
2. **Refresh Token Lookup**: Indexed by `token_hash` for O(1) lookup
3. **Token Cleanup**: Scheduled job runs during low-traffic hours (2 AM)
4. **Password Hashing**: BCrypt work factor of 12 balances security and performance
5. **Connection Pooling**: Reuse database connections for token operations

## Security Considerations

1. **Password Storage**: BCrypt with work factor 12, never store plaintext
2. **Token Rotation**: New refresh token issued on every refresh, old token revoked
3. **Token Expiration**: Short-lived access tokens (15 min), longer refresh tokens (7 days)
4. **Constant-Time Comparison**: BCrypt.checkpw prevents timing attacks
5. **HTTPS Only**: All authentication endpoints must use HTTPS in production
6. **Rate Limiting**: Implement rate limiting on login endpoint (future enhancement)
7. **Audit Logging**: All authentication events logged using SLF4J with appropriate log levels

## Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-secret-key-here-minimum-256-bits
JWT_ACCESS_TOKEN_EXPIRATION_MINUTES=15
JWT_REFRESH_TOKEN_EXPIRATION_DAYS=7

# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/regtech
DB_USERNAME=regtech_user
DB_PASSWORD=secure_password
```

### Application Properties

```yaml
iam:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration-minutes: ${JWT_ACCESS_TOKEN_EXPIRATION_MINUTES:15}
      refresh-token-expiration-days: ${JWT_REFRESH_TOKEN_EXPIRATION_DAYS:7}
    
    password:
      bcrypt-work-factor: 12
    
    token-cleanup:
      enabled: true
      cron: "0 0 2 * * *"  # Daily at 2 AM
      retention-days: 30
```

## API Documentation

### Endpoints

#### POST /api/v1/auth/login
Authenticate user with email and password.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

**Response** (Single Bank):
```json
{
  "success": true,
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "accessTokenExpiresAt": "2024-01-01T12:15:00Z",
    "refreshTokenExpiresAt": "2024-01-08T12:00:00Z",
    "requiresBankSelection": false,
    "availableBanks": [],
    "tenantContext": {
      "bankId": "bank-123",
      "bankName": "Example Bank",
      "role": "BANK_ADMIN",
      "permissions": ["BCBS239_UPLOAD_FILES", "BCBS239_VIEW_REPORTS"]
    },
    "nextStep": "DASHBOARD"
  }
}
```

**Response** (Multiple Banks):
```json
{
  "success": true,
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "accessTokenExpiresAt": "2024-01-01T12:15:00Z",
    "refreshTokenExpiresAt": "2024-01-08T12:00:00Z",
    "requiresBankSelection": true,
    "availableBanks": [
      {
        "bankId": "bank-123",
        "bankName": "Example Bank",
        "role": "BANK_ADMIN"
      },
      {
        "bankId": "bank-456",
        "bankName": "Another Bank",
        "role": "DATA_ANALYST"
      }
    ],
    "tenantContext": null,
    "nextStep": "SELECT_BANK"
  }
}
```

#### POST /api/v1/auth/refresh
Refresh access token using refresh token.

**Request**:
```json
{
  "refreshToken": "a1b2c3d4e5f6..."
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "x9y8z7w6v5u4...",
    "accessTokenExpiresAt": "2024-01-01T12:15:00Z",
    "refreshTokenExpiresAt": "2024-01-08T12:00:00Z",
    "tenantContext": {
      "bankId": "bank-123",
      "bankName": "Example Bank",
      "role": "BANK_ADMIN",
      "permissions": ["BCBS239_UPLOAD_FILES"]
    }
  }
}
```

#### POST /api/v1/auth/select-bank
Select bank context for multi-bank users.

**Request**:
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "bankId": "bank-123",
  "refreshToken": "a1b2c3d4e5f6..."
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "x9y8z7w6v5u4...",
    "accessTokenExpiresAt": "2024-01-01T12:15:00Z",
    "refreshTokenExpiresAt": "2024-01-08T12:00:00Z",
    "tenantContext": {
      "bankId": "bank-123",
      "bankName": "Example Bank",
      "role": "BANK_ADMIN",
      "permissions": ["BCBS239_UPLOAD_FILES"]
    }
  }
}
```

#### POST /api/v1/auth/logout
Logout user and revoke refresh tokens.

**Request**:
```json
{
  "refreshToken": "a1b2c3d4e5f6..."
}
```

**Response**:
```json
{
  "success": true,
  "message": "Logged out successfully",
  "messageKey": "logout.success"
}
```

## Migration Path

1. **Phase 1**: Implement domain layer (RefreshToken aggregate, value objects)
2. **Phase 2**: Implement application layer (command handlers)
3. **Phase 3**: Implement infrastructure layer (repositories, database migration)
4. **Phase 4**: Implement presentation layer (controllers, routes)
5. **Phase 5**: Update SecurityFilter to validate JWT tokens
6. **Phase 6**: Add token cleanup scheduler
7. **Phase 7**: Integration testing and security audit

## Dependencies

### New Dependencies Required

```xml
<!-- JWT Token Generation and Validation -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.1</version>
</dependency>

<!-- BCrypt Password Hashing -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

Note: These dependencies may already be present in the project. Verify before adding.

