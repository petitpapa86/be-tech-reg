# Saga Pattern Implementation Guide

## Overview

This document provides a comprehensive guide on implementing and using the Saga pattern in the RegTech application. Sagas are used to orchestrate complex business processes that span multiple bounded contexts and require reliable, transactional coordination.

## What is a Saga?

A Saga is a long-running business process that coordinates multiple services or bounded contexts. Unlike traditional ACID transactions, sagas use compensation (rollback) operations to maintain consistency across distributed systems.

### Key Characteristics
- **Event-driven**: Sagas react to events and dispatch commands
- **Compensable**: Failed operations can be rolled back
- **Distributed**: Coordinates across multiple services
- **Reliable**: Uses inbox/outbox patterns for message delivery

## Current Implementation: PaymentVerificationSaga

### Purpose
The PaymentVerificationSaga orchestrates the complete user registration and payment setup process:

1. User registers → Stripe customer creation
2. Customer created → Subscription creation
3. Subscription created → Invoice generation
4. Invoice created → Payment processing

### Architecture

#### Components

**1. Saga Class (`PaymentVerificationSaga`)**
```java
@Component
public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {
    // Event handlers and command dispatching logic
}
```

**2. Saga Data (`PaymentVerificationSagaData`)**
```java
@Data
@Builder
public class PaymentVerificationSagaData {
    private String correlationId;
    private UserId userId;
    private String userEmail;
    private String userName;
    private String paymentMethodId;
    // ... state tracking fields
}
```

**3. Command Handlers**
- `CreateStripeCustomerCommandHandler`
- `CreateStripeSubscriptionCommandHandler`
- `CreateStripeInvoiceCommandHandler`
- `FinalizeBillingAccountCommandHandler`

**4. Event Handlers**
- Integration event handlers for cross-context communication
- Domain event handlers for saga progression

### Saga Lifecycle

#### Initiation
```java
// Started by UserRegisteredEventHandler when UserRegisteredIntegrationEvent is received
PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
    .correlationId(event.getId().toString())
    .userId(new UserId(event.getUserId()))
    .userEmail(event.getEmail())
    .userName(event.getName())
    .paymentMethodId(event.getPaymentMethodId())
    .build();

SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);
```

#### State Transitions

1. **Initial State**: Saga created with user data
2. **Customer Creation**: Dispatches `CreateStripeCustomerCommand`
3. **Customer Created**: Receives `StripeCustomerCreatedEvent`, dispatches subscription command
4. **Subscription Created**: Receives `StripeSubscriptionCreatedEvent`, dispatches invoice command
5. **Invoice Created**: Receives `StripeInvoiceCreatedEvent`, dispatches payment command
6. **Payment Succeeded**: Receives `StripePaymentSucceededEvent`, finalizes billing account
7. **Completed**: All steps successful

#### Error Handling
- Each step can fail and set `failureReason`
- Saga status becomes `FAILED` if any step fails
- Compensation logic can be implemented for rollback

## How to Create a New Saga

### Step 1: Define Saga Data
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YourSagaData {
    private String correlationId;
    // Add fields for state tracking
    private String currentStep;
    private String failureReason;
}
```

### Step 2: Create Saga Class
```java
@Component
public class YourSaga extends AbstractSaga<YourSagaData> {

    public YourSaga(SagaId id, YourSagaData data) {
        super(id, "YourSaga", data);
        registerHandlers();
    }

    private void registerHandlers() {
        onEvent(SomeEvent.class, this::handleSomeEvent);
        // Register event handlers
    }

    // Event handlers
    private void handleSomeEvent(SomeEvent event) {
        // Update data
        data.setCurrentStep("completed");

        // Dispatch next command
        dispatchCommand(new NextCommand(getId(), data.getSomeValue()));

        updateStatus();
    }

    @Override
    protected void updateStatus() {
        if (data.getFailureReason() != null) {
            setStatus(SagaStatus.FAILED);
            setCompletedAt(Instant.now());
        } else if (data.getCurrentStep().equals("completed")) {
            setStatus(SagaStatus.COMPLETED);
            setCompletedAt(Instant.now());
        }
    }
}
```

### Step 3: Create Commands
```java
public class YourCommand extends SagaCommand {
    public YourCommand(SagaId sagaId, String parameter) {
        super(sagaId, "YourCommand", Map.of("parameter", parameter), Instant.now());
    }

    public String getParameter() {
        return (String) payload().get("parameter");
    }
}
```

### Step 4: Create Command Handlers
```java
@Component
public class YourCommandHandler {

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(YourCommand command) {
        // Process command
        // ...

        // Publish success event
        eventPublisher.publishEvent(new YourEvent(command.getSagaId(), result));
    }
}
```

### Step 5: Create Events
```java
public class YourEvent extends SagaMessage {
    private final String result;

    public YourEvent(SagaId sagaId, String result) {
        super("YourEvent", Instant.now(), sagaId);
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
```

### Step 6: Start the Saga
```java
@Component
public class YourSagaStarter implements DomainEventHandler<TriggerEvent> {

    private final SagaManager sagaManager;

    @Override
    public boolean handle(TriggerEvent event) {
        YourSagaData data = YourSagaData.builder()
            .correlationId(event.getId().toString())
            .build();

        sagaManager.startSaga(YourSaga.class, data);
        return true;
    }
}
```

## Command Handler Patterns

### Synchronous Command Handling
```java
@Component
public class SyncCommandHandler {

    @EventListener
    public Result<Void> handle(YourCommand command) {
        // Synchronous processing
        return Result.success(null);
    }
}
```

### Asynchronous Command Handling
```java
@Component
public class AsyncCommandHandler {

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(YourCommand command) {
        // Asynchronous processing
        // Publish events on completion
    }
}
```

### Functional Command Handling
```java
@Component
public class FunctionalCommandHandler {

    public Result<CommandResult> handle(YourCommand command) {
        return processCommand(
            command,
            this::validateInput,
            this::executeBusinessLogic,
            this::publishEvent
        );
    }

    static Result<CommandResult> processCommand(
            YourCommand command,
            Function<YourCommand, Result<Void>> validator,
            Function<YourCommand, Result<BusinessResult>> processor,
            Consumer<BusinessResult> eventPublisher) {
        // Functional composition
    }
}
```

## Event Handling Patterns

### Integration Event Handlers
```java
@Component
public class IntegrationEventHandler implements DomainEventHandler<ExternalEvent> {

    @Override
    public boolean handle(ExternalEvent event) {
        // Process external events
        // Start sagas or update state
        return true; // or false on failure
    }
}
```

### Saga Event Handlers
```java
// In saga class
private void registerHandlers() {
    onEvent(YourEvent.class, this::handleYourEvent);
}

private void handleYourEvent(YourEvent event) {
    // Update saga data
    // Dispatch next command
    updateStatus();
}
```

## Best Practices

### 1. Saga Design
- **Keep sagas focused**: One saga per business process
- **Minimize saga data**: Only store essential state
- **Use correlation IDs**: For tracking across services
- **Handle failures gracefully**: Implement compensation logic

### 2. Command Design
- **Make commands immutable**: Use records or final fields
- **Include all required data**: Avoid external lookups in handlers
- **Use descriptive names**: `CreateOrderCommand`, not `ProcessCommand`

### 3. Event Design
- **Use past tense**: `OrderCreatedEvent`, not `CreateOrderEvent`
- **Include relevant data**: Events should be self-contained
- **Version events**: For schema evolution

### 4. Error Handling
- **Fail fast**: Validate inputs early
- **Log errors**: Include correlation IDs
- **Implement retries**: For transient failures
- **Use circuit breakers**: For external service calls

### 5. Testing
- **Test sagas in isolation**: Mock event publishers
- **Test command handlers**: Verify event publication
- **Test failure scenarios**: Ensure proper rollback
- **Use test data builders**: For complex saga data

### 6. Monitoring
- **Track saga metrics**: Success rates, duration
- **Log saga lifecycle**: Start, progress, completion
- **Alert on failures**: Failed sagas need attention
- **Monitor queue depths**: For inbox/outbox patterns

## Configuration

### Saga Task Executor
```java
@Configuration
@EnableAsync
public class SagaConfiguration {

    @Bean
    @Primary
    public TaskExecutor sagaTaskExecutor() {
        return new VirtualThreadTaskExecutor();
    }
}
```

### Saga Persistence
```java
@Bean
public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver(EntityManager entityManager) {
    return JpaSagaRepository.sagaSaver(entityManager, objectMapper);
}
```

## Troubleshooting

### Common Issues

1. **Saga not starting**: Check event routing and handler registration
2. **Commands not dispatching**: Verify saga status and event handling
3. **Events not received**: Check inbox processing and message serialization
4. **Infinite loops**: Ensure saga status updates prevent re-dispatching

### Debugging
- **Enable saga logging**: Track state transitions
- **Use correlation IDs**: Trace requests across services
- **Monitor saga tables**: Check persisted state
- **Test event handlers**: Isolate component failures

## Migration Guide

### From Direct Service Calls to Sagas
1. Identify the business process
2. Define saga boundaries
3. Create commands for each step
4. Implement compensation logic
5. Add event-driven coordination
6. Test end-to-end scenarios

### From Domain Events to Integration Events
1. Replace domain event handlers with integration event handlers
2. Update event publishing in source contexts
3. Implement inbox processing
4. Add retry and deduplication logic
5. Update monitoring and alerting

This guide should be updated as new patterns emerge and the saga implementation evolves.</content>
<parameter name="filePath">c:\Users\alseny\Desktop\react projects\regtech\docs\SAGA_PATTERN_IMPLEMENTATION_GUIDE.md