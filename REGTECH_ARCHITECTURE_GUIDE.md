# RegTech Application Architecture Guide

## Overview

This guide provides comprehensive instructions for implementing features in the RegTech application, which follows a **Modular Monolithic Architecture** with **Domain-Driven Design (DDD)** principles, **Functional Programming** patterns, and **Saga-based distributed transactions**.

## Table of Contents

1. [Architecture Principles](#architecture-principles)
2. [Project Structure](#project-structure)
3. [Module Organization](#module-organization)
4. [Functional Programming Patterns](#functional-programming-patterns)
5. [Domain-Driven Design Implementation](#domain-driven-design-implementation)
6. [API Design Guidelines](#api-design-guidelines)
7. [Saga Pattern Implementation](#saga-pattern-implementation)
8. [Database and Persistence](#database-and-persistence)
9. [Testing Strategy](#testing-strategy)
10. [Security and Configuration](#security-and-configuration)
11. [Monitoring and Observability](#monitoring-and-observability)
12. [Build and Deployment](#build-and-deployment)

## Architecture Principles

### Core Principles

1. **Modular Monolithic Design**: Clear module boundaries with shared infrastructure
2. **Functional Programming**: Closures, immutability, and pure functions
3. **Domain-Driven Design**: Business-focused organization and modeling
4. **Railway-Oriented Programming**: Result types for error handling
5. **Testability First**: Closure-based dependency injection for easy testing
6. **Event-Driven Communication**: Asynchronous cross-module communication

### Key Benefits

- **Testability**: Pure functions with mockable closures
- **Maintainability**: Clear boundaries and functional patterns
- **Scalability**: Modular design with event-driven communication
- **Reliability**: Explicit error handling and saga compensation

## Project Structure

```
regtech/
├── pom.xml                           # Parent POM with modules
├── regtech-core/                     # Shared core module
│   ├── src/main/java/com/bcbs239/regtech/
│   │   ├── RegtechApplication.java   # Main application entry point
│   │   └── core/
│   │       ├── shared/               # Functional types (Result, Maybe, etc.)
│   │       ├── saga/                 # Saga orchestration framework
│   │       ├── events/               # Cross-module event bus
│   │       ├── config/               # Shared configurations
│   │       ├── health/               # Health monitoring
│   │       └── web/                  # Web utilities
├── regtech-iam/                      # Identity & Access Management
├── regtech-billing/                  # Billing module
└── docs/                             # Documentation
```

## Module Organization

### DDD Layer Structure

Each module follows **Domain-Driven Design** with clear architectural layers:

```
regtech-[module]/
├── [Module]Module.java              # Module configuration
├── api/                             # REST API controllers
│   └── [aggregate]/                 # Controllers grouped by aggregate
├── application/                     # Use cases & command handlers
│   └── [usecase]/                   # CQRS command/query handlers
├── domain/                          # Domain model & business logic
│   └── [aggregate]/                 # Domain aggregates & entities
└── infrastructure/                  # External concerns
    ├── persistence/                 # JPA entities & repositories
    ├── security/                    # Security configurations
    └── health/                      # Health indicators
```

### Example: IAM Module Structure

```
regtech-iam/
├── IamModule.java
├── api/
│   └── users/
│       └── UserController.java
├── application/
│   ├── createuser/
│   │   ├── RegisterUserCommand.java
│   │   ├── RegisterUserCommandHandler.java
│   │   └── RegisterUserResponse.java
│   └── authenticate/
│       ├── AuthenticationCommand.java
│       └── AuthenticateUserCommandHandler.java
├── domain/
│   └── users/
│       ├── User.java                # Aggregate Root
│       ├── UserId.java              # Value Object
│       ├── Email.java               # Value Object
│       ├── Password.java            # Value Object
│       └── UserRepository.java      # Repository with closures
└── infrastructure/
    ├── persistence/
    │   └── JpaUserRepository.java
    └── health/
        └── IamModuleHealthIndicator.java
```

## Functional Programming Patterns

### Core Functional Types

#### Result<T> - Railway-Oriented Programming

```java
public class Result<T> {
    // Success/Failure pattern without exceptions
    public static <T> Result<T> success(T value) { ... }
    public static <T> Result<T> failure(ErrorDetail error) { ... }

    // Functional operations
    public <U> Result<U> map(Function<T, U> mapper) { ... }
    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) { ... }
}

// Usage example
Result<Email> emailResult = Email.create(emailString);
if (emailResult.isFailure()) {
    return Result.failure(emailResult.getError().get());
}
```

#### Maybe<T> - Functional Optional

```java
public sealed interface Maybe<T> {
    record Some<T>(T value) implements Maybe<T> { ... }
    record None<T>() implements Maybe<T> { ... }
    
    static <T> Maybe<T> some(T value) { return new Some<>(value); }
    static <T> Maybe<T> none() { return new None<>(); }
}

// Usage example
Maybe<User> existingUser = emailLookup.apply(email);
if (existingUser.isPresent()) {
    return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS"));
}
```

### Closure-Based Repository Pattern

```java
@Repository
public class UserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    // Functional repository operations
    public Function<Email, Maybe<User>> emailLookup() {
        return email -> {
            try {
                User user = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email.getValue())
                    .getSingleResult();
                return Maybe.some(user);
            } catch (NoResultException e) {
                return Maybe.none();
            }
        };
    }

    public Function<User, Result<UserId>> userSaver() {
        return user -> {
            try {
                if (user.getId() == null) {
                    entityManager.persist(user);
                } else {
                    user = entityManager.merge(user);
                }
                return Result.success(user.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_SAVE_FAILED",
                    "Failed to save user: " + e.getMessage()));
            }
        };
    }
}
```

### Command Handler with Closures

```java
@Component
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;

    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // Inject repository operations as closures
        return registerUser(command,
            userRepository.emailLookup(),
            userRepository.userSaver());
    }

    // Pure function with injected dependencies
    static Result<RegisterUserResponse> registerUser(
            RegisterUserCommand command,
            Function<Email, Maybe<User>> emailLookup,
            Function<User, Result<UserId>> userSaver) {
        
        // Pure business logic with no side effects
        Result<Email> emailResult = Email.create(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Email email = emailResult.getValue().get();
        Maybe<User> existingUser = emailLookup.apply(email);
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS"));
        }
        
        // Create and save user
        User newUser = User.create(email, password, firstName, lastName);
        Result<UserId> saveResult = userSaver.apply(newUser);
        
        return saveResult.map(userId -> new RegisterUserResponse(userId, correlationId));
    }
}
```

## Domain-Driven Design Implementation

### Aggregate Root Pattern

```java
/**
 * User Aggregate Root
 * Encapsulates business rules and maintains consistency
 */
public class User {
    private String id;
    private Email email;
    private Password password;
    private UserStatus status;
    private long version;

    // Private constructor - use factory methods
    private User() {}

    // Factory method for creation
    public static User create(Email email, Password password, String firstName, String lastName) {
        User user = new User();
        user.id = UserId.generate().getValue();
        user.email = email;
        user.password = password;
        user.status = UserStatus.PENDING_PAYMENT;
        user.createdAt = Instant.now();
        return user;
    }

    // Business methods
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void changePassword(Password newPassword) {
        this.password = newPassword;
        this.updatedAt = Instant.now();
        this.version++;
    }
}
```

### Value Objects

```java
/**
 * Email Value Object with validation
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static Result<Email> create(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EMAIL_REQUIRED"));
        }

        String trimmedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return Result.failure(ErrorDetail.of("EMAIL_INVALID"));
        }

        return Result.success(new Email(trimmedEmail));
    }

    public String getValue() {
        return value;
    }
}
```

### Command and Query Separation

```java
// Command - Changes state
public record RegisterUserCommand(
    String email,
    String password,
    String firstName,
    String lastName
) {
    public static Result<RegisterUserCommand> create(
            String email, String password, String firstName, String lastName) {
        // Validation logic
        return Result.success(new RegisterUserCommand(email, password, firstName, lastName));
    }
}

// Query - Reads state
public record GetUserQuery(String userId) {}

// Response DTOs
public record RegisterUserResponse(UserId userId, String correlationId) {}
public record UserDto(String id, String email, String firstName, String lastName) {}
```

## API Design Guidelines

### Unified API Response Structure

```java
/**
 * Unified API response envelope for both success and error responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final String messageKey;
    private final T data;
    private final ErrorType type;
    private final List<FieldError> errors;
    private final Map<String, Object> meta;

    // Static factory methods
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>success().data(data).build();
    }

    public static ApiResponse<Void> validationError(List<FieldError> errors) {
        return ApiResponse.error()
                .type(ErrorType.VALIDATION_ERROR)
                .errors(errors)
                .build();
    }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    private final RegisterUserCommandHandler registerUserCommandHandler;

    @PostMapping("/register")
    public ResponseEntity<? extends ApiResponse<?>> registerUser(
            @RequestBody RegisterUserRequest request) {

        // Create command with validation
        Result<RegisterUserCommand> commandResult = RegisterUserCommand.create(
            request.email(), request.password(), request.firstName(), request.lastName()
        );

        if (commandResult.isFailure()) {
            ErrorDetail error = commandResult.getError().get();
            return handleValidationError(error.getFieldErrors(), error.getMessage());
        }

        // Handle the command
        Result<RegisterUserResponse> result = registerUserCommandHandler.handle(commandResult.getValue().get());
        return handleResult(result, "User registered successfully", "user.register.success");
    }
}
```

### Error Types and Handling

```java
public enum ErrorType {
    VALIDATION_ERROR,      // Field-level validation errors
    BUSINESS_RULE_ERROR,   // Business logic violations
    SYSTEM_ERROR,          // System/infrastructure failures
    AUTHENTICATION_ERROR,  // Auth/authz issues
    NOT_FOUND_ERROR       // Resource not found
}

// Field-level validation errors
public record FieldError(
    String field,
    String code,
    String message,
    String messageKey
) {}
```

## Saga Pattern Implementation

### Saga Data Class

```java
public class UserOnboardingSagaData extends SagaData {
    private String userId;
    private String email;
    private String riskProfile;
    private OnboardingStep currentStep = OnboardingStep.IDENTITY_VERIFICATION;

    public enum OnboardingStep {
        IDENTITY_VERIFICATION,
        RISK_ASSESSMENT,
        COMPLIANCE_CHECK,
        ACCOUNT_CREATION,
        FUNDING
    }

    // Getters, setters, and business logic methods
}
```

### Saga Implementation with Closures

```java
@Component
public class UserOnboardingSaga implements Saga<UserOnboardingSagaData> {

    // Closure dependencies
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.TimeoutScheduler timeoutScheduler;
    private final SagaClosures.Logger logger;

    // Constructor with closures
    public UserOnboardingSaga(
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.TimeoutScheduler timeoutScheduler,
            SagaClosures.Logger logger) {
        this.messagePublisher = messagePublisher;
        this.timeoutScheduler = timeoutScheduler;
        this.logger = logger;
    }

    // Spring constructor (adapts services to closures)
    public UserOnboardingSaga(MessageBus messageBus,
                             BusinessTimeoutService timeoutService) {
        this(
            messageBus::publish,
            (sagaId, type, delayMs, callback) -> 
                timeoutService.scheduleTimeout(sagaId, type, Duration.ofMillis(delayMs), callback),
            (level, message, args) -> logger.info(String.format(message, args))
        );
    }

    @Override
    public SagaResult execute(UserOnboardingSagaData sagaData) {
        try {
            // Step 1: Identity verification
            performIdentityVerification(sagaData);
            
            // Step 2: Send to compliance
            sendToCompliance(sagaData);
            
            // Step 3: Schedule timeout
            scheduleComplianceTimeout(sagaData);
            
            return SagaResult.success();
        } catch (Exception e) {
            logger.log("error", "Onboarding failed: {}", e.getMessage());
            return SagaResult.failure("Onboarding failed: " + e.getMessage());
        }
    }

    @Override
    public SagaResult handleMessage(UserOnboardingSagaData sagaData, SagaMessage message) {
        switch (message.getType()) {
            case "compliance.approved":
                return handleComplianceApproval(sagaData, message);
            case "compliance.rejected":
                return handleComplianceRejection(sagaData, message);
            default:
                return SagaResult.failure("Unknown message type: " + message.getType());
        }
    }

    @Override
    public SagaResult compensate(UserOnboardingSagaData sagaData) {
        // Implement compensation logic
        cancelUserCreation(sagaData);
        notifyUserOfCancellation(sagaData);
        return SagaResult.success();
    }

    @Override
    public String getSagaType() {
        return "user-onboarding";
    }
}
```

### Starting a Saga

```java
@Service
public class UserService {

    private final SagaOrchestrator sagaOrchestrator;

    public CompletableFuture<SagaResult> onboardUser(UserDto userDto) {
        // Create saga data
        UserOnboardingSagaData sagaData = new UserOnboardingSagaData();
        sagaData.setSagaId(UUID.randomUUID().toString());
        sagaData.setUserId(userDto.getId());
        sagaData.setEmail(userDto.getEmail());

        // Create saga with closures
        UserOnboardingSaga saga = new UserOnboardingSaga(
            messageBus::publish,
            timeoutService::scheduleTimeout,
            logger::info
        );

        // Start the saga
        return sagaOrchestrator.startSaga(saga, sagaData);
    }
}
```

## Database and Persistence

### Multi-Schema Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compliance-core-app
    username: postgres
    password: dracons86
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: iam
  flyway:
    locations: classpath:db/migration
    schemas: iam,billing
    baseline-on-migrate: true

---
# Development profile
spring:
  config:
    activate:
      on-profile: development
  datasource:
    url: jdbc:h2:mem:regtech;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

### JPA Configuration

```java
@Configuration
@EntityScan(basePackages = {
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.bcbs239.regtech.iam.infrastructure.repository",
    "com.bcbs239.regtech.billing.infrastructure.repository"
})
public class ModularJpaConfiguration {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        // Configure shared data source
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

### Flyway Migrations

```sql
-- V1__Create_user_table.sql
CREATE SCHEMA IF NOT EXISTS iam;

CREATE TABLE iam.users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email ON iam.users(email);
CREATE INDEX idx_users_status ON iam.users(status);
```

## Testing Strategy

### Closure-Based Testing (No Mocks Required)

```java
@DisplayName("User Registration Tests with Closures")
class RegisterUserCommandHandlerTest {

    // Mock closures - just plain functions!
    private final AtomicReference<User> savedUser = new AtomicReference<>();
    private final Map<String, User> userStore = new ConcurrentHashMap<>();

    // Create mock closures (just functions!)
    private final Function<Email, Maybe<User>> emailLookup = email -> {
        User user = userStore.get(email.getValue());
        return user != null ? Maybe.some(user) : Maybe.none();
    };

    private final Function<User, Result<UserId>> userSaver = user -> {
        savedUser.set(user);
        userStore.put(user.getEmail().getValue(), user);
        return Result.success(user.getId());
    };

    @Test
    @DisplayName("Should register user successfully with valid data")
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com", "password123", "John", "Doe"
        );

        // When
        Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(
            command, emailLookup, userSaver
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(savedUser.get()).isNotNull();
        assertThat(savedUser.get().getEmail().getValue()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should fail when email already exists")
    void shouldFailWhenEmailExists() {
        // Given - pre-existing user
        Email email = Email.create("existing@example.com").getValue().get();
        User existingUser = User.create(email, Password.create("pass").getValue().get(), "Jane", "Doe");
        userStore.put(email.getValue(), existingUser);

        RegisterUserCommand command = new RegisterUserCommand(
            "existing@example.com", "password123", "John", "Doe"
        );

        // When
        Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(
            command, emailLookup, userSaver
        );

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().get().getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }
}
```

### Integration Testing

```java
@SpringBootTest
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterUserEndToEnd() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
            "integration@test.com", "password123", "Integration", "Test"
        );

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/users/register", request, ApiResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

### Saga Testing with Closures

```java
@DisplayName("Saga Testing with Closures")
class UserOnboardingSagaTest {

    private final StringBuilder logOutput = new StringBuilder();
    private final Map<String, SagaMessage> publishedMessages = new ConcurrentHashMap<>();

    private final SagaClosures.MessagePublisher messagePublisher = message -> {
        publishedMessages.put(message.getType(), message);
    };

    private final SagaClosures.Logger logger = (level, message, args) -> {
        logOutput.append(String.format(message, args)).append("\n");
    };

    @Test
    void shouldExecuteSagaSuccessfully() {
        // Given
        UserOnboardingSaga saga = new UserOnboardingSaga(messagePublisher, null, logger);
        UserOnboardingSagaData sagaData = new UserOnboardingSagaData();
        sagaData.setSagaId("test-saga");

        // When
        SagaResult result = saga.execute(sagaData);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(publishedMessages).containsKey("compliance.review-request");
    }
}
```

## Security and Configuration

### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/users/register").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
}
```

### Configuration Properties

```java
@ConfigurationProperties(prefix = "regtech")
public class RegtechProperties {
    
    private Database database = new Database();
    private Security security = new Security();
    private Saga saga = new Saga();

    public static class Database {
        private String defaultSchema = "iam";
        private List<String> schemas = List.of("iam", "billing");
        // getters/setters
    }

    public static class Security {
        private boolean enabled = true;
        private OAuth2 oauth2 = new OAuth2();
        // getters/setters
    }

    public static class Saga {
        private Duration defaultTimeout = Duration.ofHours(24);
        private boolean monitoringEnabled = true;
        // getters/setters
    }
}
```

## Monitoring and Observability

### Health Indicators

```java
@Component
public class IamModuleHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    @Override
    public Health health() {
        try {
            // Check database connectivity
            userRepository.emailLookup().apply(Email.create("health@check.com").getValue().get());
            
            return Health.up()
                .withDetail("module", "iam")
                .withDetail("database", "connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("module", "iam")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Correlation ID Tracking

```java
@Component
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String correlationId = extractOrGenerateCorrelationId(request);
        
        try (MDCCloseable mdcCloseable = MDC.putCloseable("correlationId", correlationId)) {
            chain.doFilter(request, response);
        }
    }
}
```

### Saga Monitoring

```java
@Component
public class SagaMonitoringService implements MonitoringService {

    @Override
    public void recordSagaStarted(String sagaId, String sagaType) {
        // Record metrics
        meterRegistry.counter("saga.started", "type", sagaType).increment();
        
        // Log event
        logger.info("Saga started: {} ({})", sagaId, sagaType);
    }

    @Override
    public void recordSagaCompleted(String sagaId, String sagaType, Duration duration) {
        meterRegistry.timer("saga.duration", "type", sagaType).record(duration);
        logger.info("Saga completed: {} ({}) in {}", sagaId, sagaType, duration);
    }
}
```

## Build and Deployment

### Maven Multi-Module Configuration

```xml
<!-- Parent POM -->
<project>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <properties>
        <java.version>25</java.version>
        <spring-boot.version>3.5.6</spring-boot.version>
    </properties>
    
    <modules>
        <module>regtech-core</module>
        <module>regtech-iam</module>
        <module>regtech-billing</module>
    </modules>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <enablePreview>true</enablePreview>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Module POM Configuration

```xml
<!-- Module POM (e.g., regtech-iam) -->
<project>
    <parent>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>regtech-iam</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.bcbs239</groupId>
            <artifactId>regtech-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>true</skip> <!-- Don't repackage library modules -->
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Build Commands

```bash
# Clean and build all modules
./mvnw clean install

# Run the application
./mvnw spring-boot:run -pl regtech-core

# Run tests
./mvnw test

# Build specific module
./mvnw clean install -pl regtech-iam
```

## Implementation Checklist

When implementing a new feature, follow this checklist:

### 1. Module Setup
- [ ] Create module directory structure following DDD layers
- [ ] Add module to parent POM
- [ ] Create module configuration class
- [ ] Add to main application component scan

### 2. Domain Layer
- [ ] Define aggregate roots with factory methods
- [ ] Create value objects with validation
- [ ] Implement repository interfaces with closures
- [ ] Add domain events if needed

### 3. Application Layer
- [ ] Create command/query DTOs with validation
- [ ] Implement command handlers using pure functions
- [ ] Add response DTOs
- [ ] Implement saga if cross-module coordination needed

### 4. Infrastructure Layer
- [ ] Implement JPA repositories
- [ ] Add Flyway migrations
- [ ] Create health indicators
- [ ] Add security configurations if needed

### 5. API Layer
- [ ] Create REST controllers extending BaseController
- [ ] Use unified ApiResponse structure
- [ ] Implement proper error handling
- [ ] Add request/response DTOs

### 6. Testing
- [ ] Write unit tests using closure injection
- [ ] Add integration tests with Spring Boot
- [ ] Test saga flows if applicable
- [ ] Add performance tests if needed

### 7. Configuration
- [ ] Add configuration properties
- [ ] Configure security rules
- [ ] Set up monitoring and logging
- [ ] Add health checks

This architecture guide provides the foundation for building maintainable, testable, and scalable features in the RegTech application. Follow these patterns consistently to ensure code quality and system reliability.