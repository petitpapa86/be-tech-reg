# Design Document

## Overview

This design document outlines the implementation of a functional repository pattern that eliminates pass-through wrapper methods and provides direct, composable access to repository functions. The design transforms repository access from imperative wrapper patterns to functional composition patterns that are more explicit, maintainable, and aligned with functional programming principles.

The core concept replaces wrapper methods like `savePayment(payment)` with functional composition like `repositoryFunctions.paymentRepository().savePayment().apply(payment)`, making the call chain explicit and eliminating unnecessary abstraction layers.

## Architecture

### Current Architecture Problems

The current repository pattern suffers from several issues:

1. **Pass-through Methods**: Repository implementations contain wrapper methods that just delegate to other repositories
2. **Code Duplication**: Method signatures are duplicated across wrapper classes
3. **Hidden Dependencies**: The call chain doesn't clearly show which repository is being accessed
4. **Maintenance Overhead**: Changes to repository interfaces require updates to multiple wrapper classes

### New Functional Architecture

The new architecture introduces three key components:

1. **Repository Functions Aggregator**: A simple dependency aggregator that provides access to repository instances
2. **Direct Repository Access**: Repositories expose their functions directly without wrapper methods
3. **Functional Composition**: Operations are composed using method references and functional interfaces

```mermaid
graph TD
    A[Handler] --> B[RepositoryFunctions]
    B --> C[PaymentRepository]
    B --> D[PaymentAttemptRepository]
    B --> E[InvoiceRepository]
    
    A --> F[Functional Composition]
    F --> G[repositoryFunctions.paymentRepository()]
    G --> H[.savePayment()]
    H --> I[.apply(payment)]
    
    style F fill:#e1f5fe
    style G fill:#e8f5e8
    style H fill:#fff3e0
    style I fill:#fce4ec
```

## Components and Interfaces

### 1. Repository Functions Aggregator

The aggregator acts as a simple dependency container that provides access to repository instances:

```java
@Component
public class PaymentRepositoryFunctions {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentValidationRepository validationRepository;
    
    public PaymentRepositoryFunctions(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentValidationRepository validationRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.validationRepository = validationRepository;
    }
    
    public PaymentRepository paymentRepository() {
        return paymentRepository;
    }
    
    public PaymentAttemptRepository paymentAttemptRepository() {
        return paymentAttemptRepository;
    }
    
    public PaymentValidationRepository validationRepository() {
        return validationRepository;
    }
}
```

### 2. Functional Repository Interface

Repositories expose their operations as functions that can be composed:

```java
public interface PaymentRepository {
    Function<Payment, Result<Payment, PaymentError>> savePayment();
    Function<PaymentId, Result<Optional<Payment>, PaymentError>> findById();
    Function<Integer, Result<List<Payment>, PaymentError>> findRetryableFailedPayments();
    Function<PaymentCriteria, Result<List<Payment>, PaymentError>> findByCriteria();
}
```

### 3. Repository Implementation

Repository implementations provide the actual function instances:

```java
@Repository
@Transactional
public class PaymentRepositoryImpl implements PaymentRepository {
    private final JpaPaymentRepository jpaRepository;
    private static final Logger logger = LoggerFactory.getLogger(PaymentRepositoryImpl.class);
    
    @Override
    public Function<Payment, Result<Payment, PaymentError>> savePayment() {
        return payment -> {
            try {
                logger.info("Saving payment with ID: {}", payment.getId());
                PaymentEntity entity = PaymentMapper.toEntity(payment);
                PaymentEntity saved = jpaRepository.save(entity);
                Payment result = PaymentMapper.toDomain(saved);
                return new Success<>(result);
            } catch (Exception e) {
                logger.error("Failed to save payment: {}", e.getMessage(), e);
                return new Failure<>(new PaymentError(SAVE_FAILED, e.getMessage()));
            }
        };
    }
    
    @Override
    public Function<PaymentId, Result<Optional<Payment>, PaymentError>> findById() {
        return paymentId -> {
            try {
                Optional<PaymentEntity> entity = jpaRepository.findById(paymentId.value());
                Optional<Payment> result = entity.map(PaymentMapper::toDomain);
                return new Success<>(result);
            } catch (Exception e) {
                logger.error("Failed to find payment by ID: {}", e.getMessage(), e);
                return new Failure<>(new PaymentError(FIND_FAILED, e.getMessage()));
            }
        };
    }
}
```

### 4. Usage in Handlers

Handlers use the functional composition pattern to access repository operations:

```java
@Component
public class ProcessPaymentHandler {
    private final PaymentRepositoryFunctions repositoryFunctions;
    
    public ProcessPaymentHandler(PaymentRepositoryFunctions repositoryFunctions) {
        this.repositoryFunctions = repositoryFunctions;
    }
    
    public Result<ProcessPaymentResponse, PaymentError> handle(ProcessPaymentCommand command) {
        return createPayment(command)
            .flatMap(repositoryFunctions.paymentRepository().savePayment())
            .flatMap(this::recordPaymentAttempt)
            .map(this::createResponse);
    }
    
    private Result<Payment, PaymentError> recordPaymentAttempt(Payment payment) {
        PaymentAttempt attempt = createPaymentAttempt(payment);
        return repositoryFunctions.paymentAttemptRepository()
            .saveAttempt()
            .apply(attempt)
            .map(savedAttempt -> payment);
    }
}
```

## Data Models

### Function Composition Types

The design uses standard Java functional interfaces:

```java
// Basic repository operation
Function<Input, Result<Output, Error>>

// Query operations
Function<Criteria, Result<List<Entity>, Error>>

// Existence checks
Function<Id, Result<Boolean, Error>>

// Batch operations
Function<List<Input>, Result<List<Output>, Error>>
```

### Method Reference Support

The pattern supports method references for cleaner composition:

```java
// Direct application
repositoryFunctions.paymentRepository().savePayment().apply(payment);

// Method reference usage
Function<Payment, Result<Payment, PaymentError>> saveOperation = 
    repositoryFunctions.paymentRepository()::savePayment;

// Functional composition
payment.flatMap(repositoryFunctions.paymentRepository().savePayment())
       .flatMap(repositoryFunctions.auditRepository().logPayment());
```

## Error Handling

### Consistent Error Patterns

All repository functions return `Result<T, E>` types for consistent error handling:

```java
public Function<Payment, Result<Payment, PaymentError>> savePayment() {
    return payment -> {
        try {
            // Implementation
            return new Success<>(savedPayment);
        } catch (DataAccessException e) {
            return new Failure<>(new PaymentError(DATABASE_ERROR, e.getMessage()));
        } catch (ValidationException e) {
            return new Failure<>(new PaymentError(VALIDATION_ERROR, e.getMessage()));
        }
    };
}
```

### Error Composition

Errors compose naturally with the Result pattern:

```java
public Result<ProcessPaymentResponse, PaymentError> processPayment(ProcessPaymentCommand command) {
    return validatePayment(command)
        .flatMap(repositoryFunctions.paymentRepository().savePayment())
        .flatMap(repositoryFunctions.notificationRepository().sendConfirmation())
        .map(this::createSuccessResponse);
}
```

## Testing Strategy

### Unit Testing Repository Functions

Repository functions can be tested in isolation:

```java
@Test
void savePayment_shouldReturnSuccess_whenValidPayment() {
    // Given
    Payment payment = createValidPayment();
    PaymentRepositoryImpl repository = new PaymentRepositoryImpl(mockJpaRepository);
    
    // When
    Result<Payment, PaymentError> result = repository.savePayment().apply(payment);
    
    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue()).isEqualTo(payment);
}
```

### Integration Testing with Functions

Integration tests can verify the complete functional composition:

```java
@Test
void processPayment_shouldSaveAndNotify_whenValidCommand() {
    // Given
    ProcessPaymentCommand command = createValidCommand();
    
    // When
    Result<ProcessPaymentResponse, PaymentError> result = handler.handle(command);
    
    // Then
    assertThat(result.isSuccess()).isTrue();
    verify(paymentRepository).savePayment();
    verify(notificationRepository).sendConfirmation();
}
```

### Mocking Functional Repositories

Repository functions can be easily mocked:

```java
@Mock
private PaymentRepositoryFunctions repositoryFunctions;

@Mock
private PaymentRepository paymentRepository;

@BeforeEach
void setUp() {
    when(repositoryFunctions.paymentRepository()).thenReturn(paymentRepository);
    when(paymentRepository.savePayment()).thenReturn(payment -> new Success<>(payment));
}
```

## Migration Strategy

### Phase 1: Create Repository Functions Aggregators

1. Create `*RepositoryFunctions` classes for each domain area
2. Inject existing repository implementations
3. Provide getter methods for repository access

### Phase 2: Convert Repository Interfaces to Functional

1. Update repository interfaces to return `Function<Input, Result<Output, Error>>`
2. Implement function-returning methods in repository implementations
3. Maintain backward compatibility by keeping existing methods temporarily

### Phase 3: Update Handler Usage

1. Replace direct repository calls with functional composition
2. Update handlers to use `repositoryFunctions.repository().operation().apply(input)`
3. Remove pass-through wrapper methods

### Phase 4: Clean Up Legacy Code

1. Remove old non-functional repository methods
2. Delete pass-through wrapper classes
3. Update tests to use the new functional pattern

## Performance Considerations

### Function Instance Reuse

Repository implementations should reuse function instances to avoid unnecessary allocations:

```java
public class PaymentRepositoryImpl implements PaymentRepository {
    private final Function<Payment, Result<Payment, PaymentError>> savePaymentFunction;
    
    public PaymentRepositoryImpl(JpaPaymentRepository jpaRepository) {
        this.savePaymentFunction = payment -> {
            // Implementation
        };
    }
    
    @Override
    public Function<Payment, Result<Payment, PaymentError>> savePayment() {
        return savePaymentFunction;
    }
}
```

### JVM Optimization

Method references and lambda expressions are optimized by the JVM and should perform comparably to direct method calls.

### Memory Usage

The functional pattern should not introduce significant memory overhead as functions are lightweight and can be reused.

## Integration Points

### Spring Integration

The pattern integrates seamlessly with Spring's dependency injection:

```java
@Configuration
public class RepositoryConfiguration {
    
    @Bean
    public PaymentRepositoryFunctions paymentRepositoryFunctions(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository) {
        return new PaymentRepositoryFunctions(paymentRepository, attemptRepository);
    }
}
```

### Transaction Management

Spring's `@Transactional` annotations work normally with the functional pattern:

```java
@Repository
@Transactional
public class PaymentRepositoryImpl implements PaymentRepository {
    // Function implementations are automatically transactional
}
```

### Observability Integration

The pattern maintains compatibility with existing metrics and logging:

```java
public Function<Payment, Result<Payment, PaymentError>> savePayment() {
    return payment -> {
        logger.info("Saving payment with correlation ID: {}", CorrelationId.current());
        metricsCollector.incrementPaymentSaves();
        // Implementation
    };
}
```