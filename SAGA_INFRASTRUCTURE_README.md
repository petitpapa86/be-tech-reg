# Saga Infrastructure Documentation

## Overview

The Saga Infrastructure provides a robust framework for implementing distributed transactions across bounded contexts in the regtech modular monolith. It follows the Saga pattern to ensure data consistency and provides automatic compensation when operations fail.

**Key Innovation: Closure-Based Architecture**

This implementation uses functional closures instead of concrete service dependencies, enabling:
- **Testability without mocks**: Direct function injection for testing
- **Functional programming**: Pure functions for business logic
- **Dependency injection**: Clean separation of concerns
- **Immutability**: Reduced side effects and easier reasoning

## What is the Saga Pattern?

The Saga pattern is a design pattern for managing distributed transactions in microservices or modular architectures. Instead of using traditional ACID transactions that span multiple services, sagas break down complex business processes into smaller, independent steps that can be compensated if something goes wrong.

### Key Concepts

- **Saga**: A sequence of local transactions that together form a distributed business process
- **Compensation**: The action taken to undo a completed step when a later step fails
- **Orchestration**: Centralized coordination of saga steps (vs. choreography with events)
- **Timeout**: Business-level timeouts for long-running processes
- **Closures**: Functional interfaces for IO, DB, and system operations

## Architecture

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Saga Data     │    │  Saga Orchestrator│    │   Message Bus   │
│   (State)       │◄──►│   (Coordinator)   │◄──►│ (Communication) │
│                 │    │   with Closures   │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         ▲                       ▲                       ▲
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Saga Closures  │    │ Monitoring Closures│    │Timeout Closures │
│   (Functional)  │    │   (Functional)    │    │   (Functional)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Component Responsibilities

- **SagaData**: Base class containing saga state, status, and metadata
- **SagaOrchestrator**: Coordinates saga execution with injected closures
- **SagaClosures**: Functional interfaces for all operations (IO, DB, monitoring, etc.)
- **MessageBus**: Enables communication between bounded contexts
- **SagaRepository**: Persists saga state for recovery and auditing
- **MonitoringService**: Provides observability through closure functions
- **BusinessTimeoutService**: Manages timeouts through closure functions

## Closure-Based Design

### Functional Interfaces

The infrastructure defines closures for all external operations:

```java
// IO Operations
MessagePublisher messagePublisher = message -> bus.publish(message);
Logger logger = (level, message, args) -> System.out.printf(message, args);

// Database Operations
SagaDataSaver sagaSaver = data -> repository.save(data);
SagaDataFinder sagaFinder = id -> Optional.ofNullable(repository.findById(id));

// Monitoring Operations
SagaEventRecorder eventRecorder = (type, sagaId, sagaType, details) ->
    monitoring.recordSagaEvent(type, sagaId, sagaType);

// System Operations
Clock clock = () -> Instant.now();
TimeoutScheduler timeoutScheduler = (sagaId, type, delayMs, callback) ->
    timeoutService.scheduleTimeout(sagaId, type, Duration.ofMillis(delayMs), callback);
```

### Benefits of Closures

1. **No Mocking Required**: Inject functions directly in tests
2. **Pure Functions**: Easier to test and reason about
3. **Dependency Injection**: Clean separation without Spring complexity
4. **Functional Composition**: Combine and transform operations easily
5. **Testability**: Deterministic behavior with controlled inputs

## How to Implement a Saga

### 1. Create Saga Data Class

Each module should define its own saga data class extending `SagaData`:

```java
package com.bcbs239.regtech.iam.saga;

import com.bcbs239.regtech.core.saga.SagaData;
import java.math.BigDecimal;

public class UserOnboardingSagaData extends SagaData {

    // Business-specific fields
    private String userId;
    private String email;
    private String riskProfile;
    private BigDecimal initialDeposit;

    // Status tracking
    private OnboardingStep currentStep = OnboardingStep.IDENTITY_VERIFICATION;

    public enum OnboardingStep {
        IDENTITY_VERIFICATION,
        RISK_ASSESSMENT,
        COMPLIANCE_CHECK,
        ACCOUNT_CREATION,
        FUNDING
    }

    // Getters and setters...
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public OnboardingStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(OnboardingStep currentStep) { this.currentStep = currentStep; }

    // Business logic methods
    public boolean isHighRisk() {
        return "HIGH".equals(riskProfile);
    }

    public boolean isComplianceApproved() {
        return getMetadata("complianceApproved") != null;
    }
}
```

### 2. Implement the Saga with Closures

Create a saga class that accepts closures as dependencies:

```java
package com.bcbs239.regtech.iam.saga;

import com.bcbs239.regtech.core.saga.*;
import org.springframework.stereotype.Component;

@Component
public class UserOnboardingSaga implements Saga<UserOnboardingSagaData> {

    // Closure dependencies
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.TimeoutScheduler timeoutScheduler;
    private final SagaClosures.SagaStepRecorder stepRecorder;
    private final SagaClosures.Logger logger;

    // Constructor with closures
    public UserOnboardingSaga(
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.TimeoutScheduler timeoutScheduler,
            SagaClosures.SagaStepRecorder stepRecorder,
            SagaClosures.Logger logger) {
        this.messagePublisher = messagePublisher;
        this.timeoutScheduler = timeoutScheduler;
        this.stepRecorder = stepRecorder;
        this.logger = logger;
    }

    // Spring constructor (adapts services to closures)
    public UserOnboardingSaga(MessageBus messageBus,
                             BusinessTimeoutService timeoutService,
                             MonitoringService monitoringService) {
        this(
            messageBus::publish,
            (sagaId, type, delayMs, callback) -> {
                timeoutService.scheduleTimeout(sagaId, type, Duration.ofMillis(delayMs), callback);
                return sagaId + "-timeout-" + type;
            },
            (sagaId, stepName, success, durationMs, details) ->
                monitoringService.recordSagaStep(sagaId, stepName, success, durationMs),
            (level, message, args) -> logger.info(String.format(message.replace("{}", "%s"), args))
        );
    }

    @Override
    public SagaResult execute(UserOnboardingSagaData sagaData) {
        try {
            long startTime = System.nanoTime();

            // Step 1: Identity verification
            performIdentityVerification(sagaData);
            stepRecorder.recordStep(sagaData.getSagaId(), "identity-verification", true,
                                  (System.nanoTime() - startTime) / 1_000_000);

            // Step 2: Risk assessment
            performRiskAssessment(sagaData);
            stepRecorder.recordStep(sagaData.getSagaId(), "risk-assessment", true,
                                  (System.nanoTime() - startTime) / 1_000_000);

            // Step 3: Send to compliance for review
            sendToCompliance(sagaData);

            // Step 4: Schedule timeout for compliance review
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
            case "compliance.timeout":
                return handleTimeout(sagaData);
            default:
                return SagaResult.failure("Unknown message type: " + message.getType());
        }
    }

    @Override
    public SagaResult compensate(UserOnboardingSagaData sagaData) {
        // Implement compensation logic using closures
        cancelUserCreation(sagaData);
        notifyUserOfCancellation(sagaData);
        return SagaResult.success();
    }

    @Override
    public String getSagaType() {
        return "user-onboarding";
    }

    private void performIdentityVerification(UserOnboardingSagaData sagaData) {
        // Implementation...
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.IDENTITY_VERIFICATION);
    }

    private void performRiskAssessment(UserOnboardingSagaData sagaData) {
        // Implementation...
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.RISK_ASSESSMENT);
    }

    private void sendToCompliance(UserOnboardingSagaData sagaData) {
        SagaMessage message = SagaMessage.builder()
                .sagaId(sagaData.getSagaId())
                .type("compliance.review-request")
                .source("iam-saga")
                .target("compliance-module")
                .payload(Map.of(
                        "userId", sagaData.getUserId(),
                        "riskProfile", sagaData.getRiskProfile()
                ))
                .build();

        messagePublisher.publish(message);
    }

    private void scheduleComplianceTimeout(UserOnboardingSagaData sagaData) {
        timeoutScheduler.schedule(
                sagaData.getSagaId(),
                "compliance-review",
                Duration.ofHours(24).toMillis(), // 24-hour review deadline
                () -> handleTimeout(sagaData)
        );
    }

    private SagaResult handleComplianceApproval(UserOnboardingSagaData sagaData, SagaMessage message) {
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.COMPLIANCE_CHECK);
        createUserAccount(sagaData);
        return SagaResult.success();
    }

    private SagaResult handleComplianceRejection(UserOnboardingSagaData sagaData, SagaMessage message) {
        return SagaResult.failure("Compliance rejected user onboarding");
    }

    private SagaResult handleTimeout(UserOnboardingSagaData sagaData) {
        return SagaResult.failure("Compliance review timeout");
    }

    // Helper methods...
    private void createUserAccount(UserOnboardingSagaData sagaData) { /* ... */ }
    private void cancelUserCreation(UserOnboardingSagaData sagaData) { /* ... */ }
    private void notifyUserOfCancellation(UserOnboardingSagaData sagaData) { /* ... */ }
}
```

### 3. Start a Saga with Closures

Use the `SagaOrchestrator` with closure-based configuration:

```java
@Service
public class UserService {

    private final SagaOrchestrator sagaOrchestrator;
    private final SagaClosures.IdGenerator idGenerator;
    private final SagaClosures.Clock clock;

    public UserService(SagaOrchestrator sagaOrchestrator,
                      SagaClosures.IdGenerator idGenerator,
                      SagaClosures.Clock clock) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public CompletableFuture<SagaResult> onboardUser(UserDto userDto) {
        // Create saga data
        UserOnboardingSagaData sagaData = new UserOnboardingSagaData();
        sagaData.setSagaId(idGenerator.generate());
        sagaData.setUserId(userDto.getId());
        sagaData.setEmail(userDto.getEmail());
        sagaData.setStartedAt(clock.now());

        // Create saga with closures
        UserOnboardingSaga saga = new UserOnboardingSaga(
            messageBus::publish,
            (sagaId, type, delayMs, callback) -> timeoutService.scheduleTimeout(sagaId, type, Duration.ofMillis(delayMs), callback),
            monitoringService::recordSagaStep,
            (level, message, args) -> logger.info(String.format(message, args))
        );

        // Start the saga
        return sagaOrchestrator.startSaga(saga, sagaData);
    }
}
```

## How to Implement a Saga

### 1. Create Saga Data Class

Each module should define its own saga data class extending `SagaData`:

```java
package com.bcbs239.regtech.iam.saga;

import com.bcbs239.regtech.core.saga.SagaData;
import java.math.BigDecimal;

public class UserOnboardingSagaData extends SagaData {

    // Business-specific fields
    private String userId;
    private String email;
    private String riskProfile;
    private BigDecimal initialDeposit;

    // Status tracking
    private OnboardingStep currentStep = OnboardingStep.IDENTITY_VERIFICATION;

    public enum OnboardingStep {
        IDENTITY_VERIFICATION,
        RISK_ASSESSMENT,
        COMPLIANCE_CHECK,
        ACCOUNT_CREATION,
        FUNDING
    }

    // Getters and setters...
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public OnboardingStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(OnboardingStep currentStep) { this.currentStep = currentStep; }

    // Business logic methods
    public boolean isHighRisk() {
        return "HIGH".equals(riskProfile);
    }

    public boolean isComplianceApproved() {
        return getMetadata("complianceApproved") != null;
    }
}
```

### 2. Implement the Saga

Create a saga class implementing the `Saga<T>` interface:

```java
package com.bcbs239.regtech.iam.saga;

import com.bcbs239.regtech.core.saga.*;
import org.springframework.stereotype.Component;

@Component
public class UserOnboardingSaga implements Saga<UserOnboardingSagaData> {

    private final MessageBus messageBus;
    private final BusinessTimeoutService timeoutService;

    public UserOnboardingSaga(MessageBus messageBus, BusinessTimeoutService timeoutService) {
        this.messageBus = messageBus;
        this.timeoutService = timeoutService;
    }

    @Override
    public SagaResult execute(UserOnboardingSagaData sagaData) {
        try {
            // Step 1: Identity verification
            performIdentityVerification(sagaData);

            // Step 2: Risk assessment
            performRiskAssessment(sagaData);

            // Step 3: Send to compliance for review
            sendToCompliance(sagaData);

            // Step 4: Schedule timeout for compliance review
            scheduleComplianceTimeout(sagaData);

            return SagaResult.success();

        } catch (Exception e) {
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
            case "compliance.timeout":
                return handleTimeout(sagaData);
            default:
                return SagaResult.failure("Unknown message type: " + message.getType());
        }
    }

    @Override
    public SagaResult compensate(UserOnboardingSagaData sagaData) {
        // Implement compensation logic
        // e.g., delete created user account, cancel pending transactions
        cancelUserCreation(sagaData);
        notifyUserOfCancellation(sagaData);
        return SagaResult.success();
    }

    @Override
    public String getSagaType() {
        return "user-onboarding";
    }

    private void performIdentityVerification(UserOnboardingSagaData sagaData) {
        // Implementation...
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.IDENTITY_VERIFICATION);
    }

    private void performRiskAssessment(UserOnboardingSagaData sagaData) {
        // Implementation...
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.RISK_ASSESSMENT);
    }

    private void sendToCompliance(UserOnboardingSagaData sagaData) {
        SagaMessage message = SagaMessage.builder()
                .sagaId(sagaData.getSagaId())
                .type("compliance.review-request")
                .source("iam-saga")
                .target("compliance-module")
                .payload(Map.of(
                        "userId", sagaData.getUserId(),
                        "riskProfile", sagaData.getRiskProfile()
                ))
                .build();

        messageBus.publish(message);
    }

    private void scheduleComplianceTimeout(UserOnboardingSagaData sagaData) {
        timeoutService.scheduleTimeout(
                sagaData.getSagaId(),
                "compliance-review",
                Duration.ofHours(24), // 24-hour review deadline
                () -> handleTimeout(sagaData.getSagaId())
        );
    }

    private SagaResult handleComplianceApproval(UserOnboardingSagaData sagaData, SagaMessage message) {
        sagaData.setCurrentStep(UserOnboardingSagaData.OnboardingStep.COMPLIANCE_CHECK);
        createUserAccount(sagaData);
        return SagaResult.success();
    }

    private SagaResult handleComplianceRejection(UserOnboardingSagaData sagaData, SagaMessage message) {
        return SagaResult.failure("Compliance rejected user onboarding");
    }

    private SagaResult handleTimeout(String sagaId) {
        return SagaResult.failure("Compliance review timeout");
    }

    // Helper methods...
    private void createUserAccount(UserOnboardingSagaData sagaData) { /* ... */ }
    private void cancelUserCreation(UserOnboardingSagaData sagaData) { /* ... */ }
    private void notifyUserOfCancellation(UserOnboardingSagaData sagaData) { /* ... */ }
}
```

### 3. Start a Saga

Use the `SagaOrchestrator` to start sagas:

```java
@Service
public class UserService {

    private final SagaOrchestrator sagaOrchestrator;
    private final UserOnboardingSaga onboardingSaga;

    public CompletableFuture<SagaResult> onboardUser(UserDto userDto) {
        // Create saga data
        UserOnboardingSagaData sagaData = new UserOnboardingSagaData();
        sagaData.setSagaId(UUID.randomUUID().toString());
        sagaData.setUserId(userDto.getId());
        sagaData.setEmail(userDto.getEmail());

        // Start the saga
        return sagaOrchestrator.startSaga(onboardingSaga, sagaData);
    }
}
```

## Message Flow Example

```
IAM Module                    Compliance Module
    │                               │
    │ 1. Start User Onboarding     │
    │    Saga                      │
    │                               │
    │ 2. Send compliance.review-   │
    │    request message ─────────►│
    │                               │
    │                               │ 3. Review user
    │                               │
    │                               │ 4. Send compliance.approved
    │◄──────────────────────────────│    message
    │                               │
    │ 5. Create user account       │
    │                               │
    │ 6. Complete saga             │
    │                               │
```

## Error Handling and Compensation

When a saga step fails, the orchestrator automatically:

1. **Stops execution** of remaining steps
2. **Initiates compensation** by calling `compensate()` on the saga
3. **Updates saga status** to `COMPENSATING` or `COMPENSATION_FAILED`
4. **Records the failure** in monitoring

### Compensation Strategies

- **Semantic compensation**: Reverse the business effect (e.g., credit money back)
- **Technical compensation**: Clean up technical artifacts (e.g., delete records)
- **Notification compensation**: Inform stakeholders of the failure

## Monitoring and Observability

The saga infrastructure provides comprehensive monitoring:

```java
// Automatic metrics recorded:
- saga.started
- saga.completed
- saga.compensating
- saga.compensated
- saga.failed
- message.sent
- message.received
- timeout.triggered
```

### Custom Monitoring

```java
@Component
public class CustomMonitoringService implements MonitoringService {

    @Override
    public void recordSagaCompleted(String sagaId) {
        // Send to business dashboard
        dashboardService.recordOnboardingCompleted();

        // Log to audit trail
        auditService.logSagaCompletion(sagaId);
    }
}
```

## Configuration

### Application Properties

```yaml
# Saga infrastructure configuration
saga:
  timeout:
    default: 24h
    compliance: 48h
  monitoring:
    enabled: true
    gcp:
      project: my-regtech-project
  message-bus:
    type: in-memory  # or 'pubsub' for production
```

### Production Considerations

1. **Persistence**: Use JPA implementation of `SagaRepository` for production
2. **Message Bus**: Use GCP Pub/Sub or similar for distributed communication
3. **Monitoring**: Integrate with GCP Cloud Monitoring and Cloud Logging
4. **Timeouts**: Configure appropriate business timeouts
5. **Recovery**: Implement saga recovery on application startup

## Testing Sagas with Closures

### Testing Without Mocks

The closure-based architecture eliminates the need for complex mocking frameworks:

```java
@DisplayName("Saga Infrastructure Production Tests with Closures")
class SagaInfrastructureProductionTest {

    // Mock closures - just plain functions!
    private final AtomicReference<SagaData> savedSagaData = new AtomicReference<>();
    private final ConcurrentHashMap<String, SagaData> sagaDataStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SagaMessage> publishedMessages = new ConcurrentHashMap<>();
    private final StringBuilder logOutput = new StringBuilder();

    // Create mock closures (just functions!)
    private final SagaClosures.SagaDataSaver sagaDataSaver = sagaData -> {
        savedSagaData.set(sagaData);
        sagaDataStore.put(sagaData.getSagaId(), sagaData);
    };

    private final SagaClosures.MessagePublisher messagePublisher = message -> {
        publishedMessages.put(message.getSagaId() + "-" + message.getType(), message);
    };

    private final SagaClosures.Clock clock = () -> Instant.parse("2025-10-07T15:00:00Z");

    // Create SagaOrchestrator with mock closures
    private final SagaOrchestrator sagaOrchestrator = new SagaOrchestrator(
        sagaDataSaver,
        sagaId -> Optional.ofNullable(sagaDataStore.get(sagaId)),
        messagePublisher,
        (eventType, sagaId, sagaType, details) -> logOutput.append("EVENT: ").append(eventType).append("\n"),
        (sagaId, stepName, success, durationMs, details) -> logOutput.append("STEP: ").append(stepName).append("\n"),
        (sagaId, messageType, direction, source, target, details) -> logOutput.append("MSG: ").append(messageType).append("\n"),
        (sagaId, timeoutType, delayMs, callback) -> sagaId + "-timeout-" + timeoutType,
        timeoutId -> logOutput.append("TIMEOUT_CANCELED: ").append(timeoutId).append("\n"),
        clock,
        (level, message, args) -> logOutput.append(level.toUpperCase()).append(": ").append(String.format(message, args)).append("\n")
    );

    @Test
    @DisplayName("Should execute saga successfully with closures")
    void shouldExecuteSagaSuccessfullyWithClosures() throws Exception {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-success");
        TestSaga saga = new TestSaga();

        // When
        CompletableFuture<SagaResult> future = sagaOrchestrator.startSaga(saga, sagaData);
        SagaResult result = future.get(); // Wait for completion

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(sagaData.getStatus()).isEqualTo(SagaData.SagaStatus.COMPLETED);
        assertThat(savedSagaData.get()).isEqualTo(sagaData);

        // Verify logging via closures
        String logs = logOutput.toString();
        assertThat(logs).contains("INFO: Starting saga: test-saga-success");
        assertThat(logs).contains("EVENT: started");
        assertThat(logs).contains("EVENT: completed");
    }

    @Test
    @DisplayName("Should handle saga failure and compensation with closures")
    void shouldHandleSagaFailureAndCompensationWithClosures() throws Exception {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-failure");
        TestSaga failingSaga = new TestSaga() {
            @Override
            public SagaResult execute(TestSagaData sagaData) {
                throw new RuntimeException("Simulated failure");
            }
        };

        // When
        CompletableFuture<SagaResult> future = sagaOrchestrator.startSaga(failingSaga, sagaData);
        SagaResult result = future.get();

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo("Simulated failure");
        assertThat(sagaData.getStatus()).isIn(SagaData.SagaStatus.COMPENSATING, SagaData.SagaStatus.COMPENSATED);

        // Verify compensation logging
        String logs = logOutput.toString();
        assertThat(logs).contains("EVENT: compensating");
    }
}
```

### Benefits of Closure-Based Testing

1. **No Mockito Required**: Direct function injection instead of mocking
2. **Deterministic**: Control exact behavior of all dependencies
3. **Fast**: No reflection or proxy generation overhead
4. **Clear**: Test code shows exactly what functions do
5. **Maintainable**: Changes to interfaces don't break tests

### Integration Testing

For integration tests with real services, use the Spring constructor:

```java
@SpringBootTest
class SagaIntegrationTest {

    @Autowired
    private SagaOrchestrator orchestrator; // Uses real services via Spring

    @Test
    void shouldHandleFullSagaFlow() {
        // Test with real message bus, database, etc.
    }
}
```

## Best Practices

### Closure-Based Development

1. **Functional Interfaces**: Define clear contracts for all operations
2. **Pure Functions**: Keep business logic free of side effects
3. **Dependency Injection**: Inject closures rather than concrete classes
4. **Testability First**: Design for easy testing with function injection
5. **Composition**: Combine closures to create complex behaviors

### Saga Implementation

1. **Idempotency**: Ensure saga steps are idempotent
2. **Timeouts**: Set appropriate business timeouts
3. **Monitoring**: Monitor saga metrics and set up alerts
4. **Compensation**: Implement robust compensation logic
5. **Documentation**: Document saga flows and compensation logic

### Testing Strategy

1. **Unit Tests**: Test saga logic with mock closures (no Mockito needed)
2. **Integration Tests**: Test with real services via Spring injection
3. **Property Tests**: Use closures to test edge cases and invariants
4. **Performance Tests**: Test with realistic closure implementations

## Migration Guide

### From Traditional to Closure-Based

**Before (Traditional):**
```java
@Component
public class MySaga implements Saga<MySagaData> {

    @Autowired
    private MessageBus messageBus;

    @Autowired
    private MonitoringService monitoring;

    public SagaResult execute(MySagaData data) {
        messageBus.publish(message);
        monitoring.recordStep("step1", true, 100);
    }
}
```

**After (Closure-Based):**
```java
public class MySaga implements Saga<MySagaData> {

    private final MessagePublisher publisher;
    private final SagaStepRecorder recorder;

    public MySaga(MessagePublisher publisher, SagaStepRecorder recorder) {
        this.publisher = publisher;
        this.recorder = recorder;
    }

    // Spring adapter constructor
    public MySaga(MessageBus messageBus, MonitoringService monitoring) {
        this(messageBus::publish, monitoring::recordSagaStep);
    }

    public SagaResult execute(MySagaData data) {
        publisher.publish(message);
        recorder.recordStep(data.getSagaId(), "step1", true, 100);
    }
}
```

### Benefits of Migration

- **Easier Testing**: Inject functions directly instead of mocking
- **Better Isolation**: Pure functions with controlled dependencies
- **Functional Programming**: Leverage Java's functional features
- **Reduced Complexity**: No need for complex mocking frameworks