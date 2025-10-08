# Bounded Context Implementation Patterns

This document provides concrete implementation patterns and code examples for building bounded contexts following the established architecture.

## Table of Contents

1. [Domain Layer Patterns](#domain-layer-patterns)
2. [Application Layer Patterns](#application-layer-patterns)
3. [Infrastructure Layer Patterns](#infrastructure-layer-patterns)
4. [API Layer Patterns](#api-layer-patterns)
5. [Cross-Context Integration Patterns](#cross-context-integration-patterns)
6. [Error Handling Patterns](#error-handling-patterns)
7. [Testing Patterns](#testing-patterns)

## Domain Layer Patterns

### Aggregate Root Pattern

```java
// Example: User aggregate in IAM context
package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.time.Instant;
import java.util.Objects;

public class User {
    private UserId id;
    private String email;
    private String name;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Package-private constructor for JPA
    User() {}

    // Private constructor for factory methods
    private User(UserId id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = UserStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 0;
    }

    // Factory method with validation
    public static Result<User> create(String email, String name) {
        if (email == null || email.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_EMAIL", 
                "Email cannot be null or empty", "user.email.required"));
        }
        
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_NAME", 
                "Name cannot be null or empty", "user.name.required"));
        }

        UserId id = UserId.generate();
        return Result.success(new User(id, email.trim(), name.trim()));
    }

    // Business methods
    public Result<Void> activate() {
        if (status == UserStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("ALREADY_ACTIVE", 
                "User is already active", "user.already.active"));
        }
        
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    public Result<Void> suspend(String reason) {
        if (status == UserStatus.SUSPENDED) {
            return Result.failure(ErrorDetail.of("ALREADY_SUSPENDED", 
                "User is already suspended", "user.already.suspended"));
        }
        
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    // Getters
    public UserId getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### Value Object Pattern

```java
// Example: UserId value object
package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;
import java.util.UUID;

public record UserId(String value) {
    
    public UserId {
        Objects.requireNonNull(value, "UserId value cannot be null");
    }
    
    public static UserId generate() {
        return new UserId("user-" + UUID.randomUUID().toString());
    }
    
    public static Result<UserId> fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_USER_ID", 
                "UserId cannot be null or empty", "user.id.invalid"));
        }
        return Result.success(new UserId(value.trim()));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Domain Event Pattern

```java
// Example: Domain event
package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.events.BaseEvent;

public class UserRegisteredEvent extends BaseEvent {
    
    private final UserId userId;
    private final String email;
    private final String name;
    
    public UserRegisteredEvent(UserId userId, String email, String name, String correlationId) {
        super(correlationId, "iam");
        this.userId = userId;
        this.email = email;
        this.name = name;
    }
    
    public UserId getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    
    @Override
    public String toString() {
        return String.format("UserRegisteredEvent{userId=%s, email=%s, correlationId=%s}", 
            userId, email, getCorrelationId());
    }
}
```

### Closure-Based Repository Pattern

Instead of traditional repository interfaces, the RegTech system uses closure-based functional repositories in the infrastructure layer:

```java
// Example: Closure-based repository (no interface needed)
package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.function.Function;

@Repository
@Transactional
public class JpaUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // Closure for finding users by ID
    public Function<UserId, Maybe<User>> userFinder() {
        return userId -> {
            try {
                UserEntity entity = entityManager.find(UserEntity.class, userId.value());
                if (entity == null) {
                    return Maybe.none();
                }
                return Maybe.some(entity.toDomain());
            } catch (Exception e) {
                return Maybe.none();
            }
        };
    }

    // Closure for finding users by email
    public Function<String, Maybe<User>> userByEmailFinder() {
        return email -> {
            try {
                UserEntity entity = entityManager.createQuery(
                    "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                    .setParameter("email", email)
                    .getSingleResult();
                return Maybe.some(entity.toDomain());
            } catch (Exception e) {
                return Maybe.none();
            }
        };
    }

    // Closure for saving users
    public Function<User, Result<UserId>> userSaver() {
        return user -> {
            try {
                UserEntity entity = UserEntity.fromDomain(user);
                
                if (user.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush();
                return Result.success(UserId.fromString(entity.getId()).getValue().get());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_SAVE_FAILED",
                    "Failed to save user: " + e.getMessage()));
            }
        };
    }

    // Traditional methods can coexist for complex queries
    public List<User> findByStatus(UserStatus status) {
        return entityManager.createQuery(
            "SELECT u FROM UserEntity u WHERE u.status = :status", UserEntity.class)
            .setParameter("status", status)
            .getResultList()
            .stream()
            .map(UserEntity::toDomain)
            .toList();
    }
}
```

## Application Layer Patterns

### Command Pattern

```java
// Example: Command
package com.bcbs239.regtech.iam.application.registeruser;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserCommand(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Bank ID is required")
    String bankId
) {
    
    public static Result<RegisterUserCommand> create(String email, String name, String bankId) {
        if (email == null || email.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EMAIL_REQUIRED", 
                "Email is required", "user.email.required"));
        }
        
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("NAME_REQUIRED", 
                "Name is required", "user.name.required"));
        }
        
        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("BANK_ID_REQUIRED", 
                "Bank ID is required", "user.bank.id.required"));
        }
        
        return Result.success(new RegisterUserCommand(
            email.trim().toLowerCase(),
            name.trim(),
            bankId.trim()
        ));
    }
}
```

### Command Handler Pattern with Closures

```java
// Example: Command handler using closure-based repositories
package com.bcbs239.regtech.iam.application.registeruser;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import com.bcbs239.regtech.iam.infrastructure.events.IamEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import org.springframework.stereotype.Component;
import java.util.function.Function;
import java.util.function.Consumer;

@Component
public class RegisterUserCommandHandler {

    private final JpaUserRepository userRepository;
    private final IamEventPublisher eventPublisher;

    public RegisterUserCommandHandler(JpaUserRepository userRepository, IamEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        return registerUser(
            command,
            userRepository.userByEmailFinder(),
            userRepository.userSaver(),
            event -> eventPublisher.publishEvent(event)
        );
    }

    // Pure function for testability - uses closures as parameters
    static Result<RegisterUserResponse> registerUser(
            RegisterUserCommand command,
            Function<String, Maybe<User>> emailChecker,
            Function<User, Result<UserId>> userSaver,
            Consumer<UserRegisteredEvent> eventPublisher) {

        // Check if user already exists using closure
        Maybe<User> existingUser = emailChecker.apply(command.email());
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("USER_ALREADY_EXISTS", 
                "User with this email already exists", "user.email.exists"));
        }

        // Create new user
        Result<User> userResult = User.create(command.email(), command.name());
        if (userResult.isFailure()) {
            return Result.failure(userResult.getError().get());
        }
        
        User user = userResult.getValue().get();

        // Save user
        Result<UserId> saveResult = userSaver.apply(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Publish domain event
        String correlationId = generateCorrelationId(user, command);
        eventPublisher.accept(new UserRegisteredEvent(
            user.getId(), 
            user.getEmail(), 
            user.getName(), 
            correlationId
        ));

        return Result.success(RegisterUserResponse.of(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStatus(),
            correlationId
        ));
    }

    private static String generateCorrelationId(User user, RegisterUserCommand command) {
        return String.format("user-registration-%s|userId=%s|email=%s|name=%s|bankId=%s",
            java.util.UUID.randomUUID(),
            user.getId().value(),
            user.getEmail(),
            user.getName(),
            command.bankId()
        );
    }
}
```

### Response Pattern

```java
// Example: Response DTO
package com.bcbs239.regtech.iam.application.registeruser;

import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserStatus;

public record RegisterUserResponse(
    UserId userId,
    String email,
    String name,
    UserStatus status,
    String correlationId
) {
    
    public static RegisterUserResponse of(UserId userId, String email, String name, 
                                        UserStatus status, String correlationId) {
        return new RegisterUserResponse(userId, email, name, status, correlationId);
    }
}
```

### Event Handler Pattern

```java
// Example: Cross-context event handler
package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.core.events.PaymentVerifiedEvent;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentVerificationEventHandler {

    private final UserRepository userRepository;

    public PaymentVerificationEventHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void handle(PaymentVerifiedEvent event) {
        // Extract user ID from correlation ID
        String correlationId = event.getCorrelationId();
        String userId = extractUserIdFromCorrelationId(correlationId);
        
        if (userId != null) {
            Result<UserId> userIdResult = UserId.fromString(userId);
            if (userIdResult.isSuccess()) {
                userRepository.findById(userIdResult.getValue().get())
                    .ifPresent(user -> {
                        Result<Void> activationResult = user.activate();
                        if (activationResult.isSuccess()) {
                            userRepository.save(user);
                        }
                    });
            }
        }
    }

    private String extractUserIdFromCorrelationId(String correlationId) {
        // Parse correlation ID to extract user ID
        // Implementation depends on correlation ID format
        return null; // Simplified for example
    }
}
```

## Infrastructure Layer Patterns

### JPA Entity Pattern

```java
// Example: JPA Entity
package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.*;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;

    // Default constructor for JPA
    public UserEntity() {}

    // Factory method from domain object
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId().value();
        entity.email = user.getEmail();
        entity.name = user.getName();
        entity.status = user.getStatus();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        entity.version = user.getVersion();
        return entity;
    }

    // Convert to domain object
    public User toDomain() {
        // Use reflection or builder pattern to reconstruct domain object
        // This is simplified for the example
        return User.create(email, name).getValue().get();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    // ... other getters and setters
}
```

### JPA Entity Conversion Pattern

```java
// Example: JPA Entity with domain conversion
package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.*;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;

    // Default constructor for JPA
    public UserEntity() {}

    // Factory method from domain object
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId().value();
        entity.email = user.getEmail();
        entity.name = user.getName();
        entity.status = user.getStatus();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        entity.version = user.getVersion();
        return entity;
    }

    // Convert to domain object
    public User toDomain() {
        // Reconstruct domain object using factory methods
        User user = User.create(email, name).getValue().get();
        // Set internal state using reflection or package-private setters
        user.setId(UserId.fromString(id).getValue().get());
        user.setStatus(status);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        user.setVersion(version);
        return user;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    // ... other getters and setters
}
```

### External Service Client Pattern

```java
// Example: External service client
package com.bcbs239.regtech.iam.infrastructure.external.notification;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationServiceClient(RestTemplate restTemplate, 
                                   @Value("${notification.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Result<Void> sendWelcomeEmail(String email, String name) {
        try {
            WelcomeEmailRequest request = new WelcomeEmailRequest(email, name);
            restTemplate.postForObject(baseUrl + "/emails/welcome", request, Void.class);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("NOTIFICATION_FAILED", 
                "Failed to send welcome email: " + e.getMessage(), 
                "notification.email.failed"));
        }
    }

    private record WelcomeEmailRequest(String email, String name) {}
}
```

## API Layer Patterns

### Controller Pattern

```java
// Example: REST Controller
package com.bcbs239.regtech.iam.api.users;

import com.bcbs239.regtech.iam.application.registeruser.*;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {

    private final RegisterUserCommandHandler registerUserHandler;

    public UserController(RegisterUserCommandHandler registerUserHandler) {
        this.registerUserHandler = registerUserHandler;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterUserResponse>> registerUser(
            @Valid @RequestBody RegisterUserRequest request) {
        
        Result<RegisterUserCommand> commandResult = RegisterUserCommand.create(
            request.email(), 
            request.name(), 
            request.bankId()
        );
        
        if (commandResult.isFailure()) {
            return badRequest(commandResult.getError().get());
        }

        Result<RegisterUserResponse> result = registerUserHandler.handle(commandResult.getValue().get());
        
        if (result.isSuccess()) {
            return created(result.getValue().get());
        } else {
            return handleError(result.getError().get());
        }
    }

    // Request/Response DTOs
    public record RegisterUserRequest(String email, String name, String bankId) {}
}
```

## Cross-Context Integration Patterns

### Event Publishing Pattern

```java
// Example: Event publisher
package com.bcbs239.regtech.iam.infrastructure.events;

import com.bcbs239.regtech.core.events.OutboxEventPublisher;
import com.bcbs239.regtech.iam.domain.users.UserRegisteredEvent;
import org.springframework.stereotype.Component;

@Component
public class IamEventPublisher {

    private final OutboxEventPublisher outboxPublisher;

    public IamEventPublisher(OutboxEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void publishEvent(Object event) {
        outboxPublisher.publish(event);
    }
}
```

### Saga Pattern

```java
// Example: Saga for cross-context coordination
package com.bcbs239.regtech.iam.application.sagas;

import com.bcbs239.regtech.core.saga.Saga;
import com.bcbs239.regtech.core.saga.SagaResult;
import org.springframework.stereotype.Component;

@Component
public class UserOnboardingSaga implements Saga<UserOnboardingSagaData> {

    @Override
    public SagaResult execute(UserOnboardingSagaData sagaData) {
        switch (sagaData.getCurrentStep()) {
            case USER_REGISTERED -> {
                return processUserRegistration(sagaData);
            }
            case PAYMENT_SETUP -> {
                return processPaymentSetup(sagaData);
            }
            case ACCOUNT_ACTIVATION -> {
                return processAccountActivation(sagaData);
            }
            default -> {
                return SagaResult.completed();
            }
        }
    }

    private SagaResult processUserRegistration(UserOnboardingSagaData sagaData) {
        // Implementation
        return SagaResult.success();
    }

    // Other step implementations...
}
```

This implementation guide provides concrete patterns that can be followed across all bounded contexts to ensure consistency and maintainability.