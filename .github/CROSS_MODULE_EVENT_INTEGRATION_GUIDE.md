# Cross-Module Event Integration Guide

## Overview

This guide explains how to publish and consume integration events across bounded contexts (modules) in the RegTech application. The system uses an event-driven architecture with the **Outbox/Inbox pattern** to ensure reliable, transactional event delivery between modules.

## Architecture Components

### 1. Core Infrastructure (regtech-core)

The core module provides the infrastructure for cross-module event communication:

```
regtech-core/
├── domain/
│   ├── events/
│   │   ├── IIntegrationEventBus.java          # Interface for publishing events
│   │   ├── IntegrationEvent.java              # Base class for outbound events
│   │   ├── DomainEvent.java                   # Base class for inbound events
│   │   └── integration/
│   │       ├── DataQualityCompletedIntegrationEvent.java  # Outbound event (publisher side)
│   │       └── DataQualityCompletedInboundEvent.java      # Inbound event (consumer side)
│   └── outbox/
│       ├── IOutboxMessageRepository.java      # Outbox repository interface
│       └── OutboxMessage.java                 # Outbox entity
└── infrastructure/
    ├── eventprocessing/
    │   ├── CrossModuleEventBus.java           # Implementation of IIntegrationEventBus
    │   ├── OutboxMessageEntity.java           # JPA entity for outbox table
    │   └── JpaOutboxMessageRepository.java    # JPA implementation
    └── inbox/
        └── InboxProcessor.java                # Processes inbox messages
```

### 2. Event Naming Convention

**CRITICAL**: Events have different names depending on their location in the flow:

- **Publishing Side (Infrastructure Layer)**:
  - Use `*IntegrationEvent` (extends `IntegrationEvent`)
  - Example: `DataQualityCompletedIntegrationEvent`
  - Location: `regtech-core/domain/events/integration/`
  
- **Consuming Side (Presentation Layer)**:
  - Use `*InboundEvent` (extends `DomainEvent`)
  - Example: `DataQualityCompletedInboundEvent`
  - Location: `regtech-core/domain/events/integration/`

**Why Two Names?**
- `IntegrationEvent` → Goes through **Outbox pattern** (serialized, persisted, published)
- `InboundEvent` → Received by **Inbox pattern** (deserialized, consumed)
- Separation allows different base classes and processing logic

## Publishing Integration Events (Producer Module)

### Step 1: Create Integration Event in Core Module

Create the **outbound** event class in `regtech-core/domain/events/integration/`:

```java
package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DataQualityCompletedIntegrationEvent extends IntegrationEvent {

    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final double overallScore;
    private final String qualityGrade;
    private final Instant completedAt;
    private final String correlationId;

    @JsonCreator
    public DataQualityCompletedIntegrationEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("overallScore") double overallScore,
            @JsonProperty("qualityGrade") String qualityGrade,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.completedAt = completedAt;
        this.correlationId = correlationId;
    }
}
```

**Key Points:**
- ✅ Extends `IntegrationEvent`
- ✅ Use `@JsonCreator` and `@JsonProperty` for Jackson serialization
- ✅ Include `correlationId` for tracing
- ✅ All fields should be `final` (immutable)
- ✅ Use `@Getter` from Lombok for getters

### Step 2: Create Inbound Event in Core Module

Create the **inbound** event class in `regtech-core/domain/events/integration/`:

```java
package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DataQualityCompletedInboundEvent extends DomainEvent {
    
    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final double overallScore;
    private final String qualityGrade;
    private final Instant completedAt;

    @JsonCreator
    public DataQualityCompletedInboundEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("overallScore") double overallScore,
            @JsonProperty("qualityGrade") String qualityGrade,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.completedAt = completedAt;
    }
    
    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
                && bankId != null && !bankId.isEmpty()
                && overallScore >= 0.0 && overallScore <= 100.0;
    }
}
```

**Key Points:**
- ✅ Extends `DomainEvent` (not IntegrationEvent)
- ✅ Same fields as IntegrationEvent version
- ✅ Optional: Add `isValid()` method for validation
- ✅ Must have same JSON structure as IntegrationEvent for deserialization

### Step 3: Create Event Publisher in Infrastructure Layer

Create a publisher in `{module}/infrastructure/messaging/outbound/`:

```java
package com.bcbs239.regtech.dataquality.infrastructure.messaging.outbound;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedIntegrationEvent;
import com.bcbs239.regtech.dataquality.domain.report.events.QualityValidationCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("dataQualityBatchQualityCompletedEventPublisher")
@RequiredArgsConstructor
public class BatchQualityCompletedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BatchQualityCompletedEventPublisher.class);

    private final IIntegrationEventBus eventBus;

    @EventListener
    public void handle(QualityValidationCompletedEvent event) {
        // Skip publishing during outbox replay to prevent duplicate events
        if (CorrelationContext.isOutboxReplay()) {
            logger.debug("Skipping integration publish for QualityValidationCompletedEvent {} because this is an outbox replay", 
                event.getEventId());
            return;
        }
        
        try {
            logger.info("Converting and publishing DataQualityCompletedIntegrationEvent for batch {}", 
                event.getBatchId().value());

            // Convert domain event to integration event
            DataQualityCompletedIntegrationEvent integrationEvent = new DataQualityCompletedIntegrationEvent(
                    event.getBatchId().value(),
                    event.getBankId().value(),
                    event.getDetailsReference().uri(),
                    event.getQualityScores().overallScore(),
                    event.getQualityGrade().getLetterGrade(),
                    Instant.now(),
                    event.getCorrelationId()
            );

            // Publish with outbox replay flag to prevent infinite loops
            ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                    .where(CorrelationContext.OUTBOX_REPLAY, true)
                    .run(() -> eventBus.publish(integrationEvent));

            logger.info("Published DataQualityCompletedIntegrationEvent for batch {}", 
                event.getBatchId().value());

        } catch (Exception ex) {
            logger.error("Failed to publish DataQualityCompletedIntegrationEvent for batch {}", 
                event.getBatchId().value(), ex);
            throw ex;
        }
    }
}
```

**Key Points:**
- ✅ Listen to **domain events** from your module with `@EventListener`
- ✅ Check `CorrelationContext.isOutboxReplay()` to prevent duplicate publishing
- ✅ Convert domain event → integration event
- ✅ Set `OUTBOX_REPLAY = true` when publishing to mark it as already processed
- ✅ Use `ScopedValue` to propagate correlation context
- ✅ Inject `IIntegrationEventBus` for publishing

### Step 4: Configuration (Already Done in Core)

The `CrossModuleEventBus` handles:
- ✅ Automatic outbox persistence
- ✅ Transaction management
- ✅ Asynchronous publishing with virtual threads
- ✅ Capacity control to prevent thread explosion

**No additional configuration needed!**

## Consuming Integration Events (Consumer Module)

### Step 1: Create Event Listener in Presentation Layer

Create a listener in `{consumer-module}/presentation/integration/listener/`:

```java
package com.bcbs239.regtech.metrics.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.metrics.application.dashboard.UpdateDashboardMetricsOnDataQualityCompletedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("metricsDataQualityCompletedIntegrationEventListener")
public class DataQualityCompletedIntegrationEventListener {

    private final UpdateDashboardMetricsOnDataQualityCompletedUseCase useCase;

    public DataQualityCompletedIntegrationEventListener(
            UpdateDashboardMetricsOnDataQualityCompletedUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(DataQualityCompletedInboundEvent event) {
        // Skip processing during inbox replay to prevent duplicate processing
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        
        // Process event with inbox replay flag set
        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}
```

**Key Points:**
- ✅ Listen to **InboundEvent** (not IntegrationEvent)
- ✅ Check `CorrelationContext.isInboxReplay()` to prevent duplicate processing
- ✅ Set `INBOX_REPLAY = true` when calling use case
- ✅ Use `ScopedValue` to propagate correlation context
- ✅ Delegate to application layer use case

### Step 2: Create Use Case in Application Layer

Create a use case in `{consumer-module}/application/`:

```java
package com.bcbs239.regtech.metrics.application.dashboard;

import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UpdateDashboardMetricsOnDataQualityCompletedUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDashboardMetricsOnDataQualityCompletedUseCase.class);

    private final IInboxMessageRepository inboxRepository;

    public UpdateDashboardMetricsOnDataQualityCompletedUseCase(
            IInboxMessageRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Transactional
    public void process(DataQualityCompletedInboundEvent event) {
        // Idempotency check using inbox pattern
        String messageId = generateMessageId(event);
        
        if (inboxRepository.existsByMessageId(messageId)) {
            logger.info("Event already processed: {}", messageId);
            return;
        }

        try {
            logger.info("Processing DataQualityCompletedInboundEvent for batch {}", event.getBatchId());

            // Business logic here - update your module's data based on the event
            // Example: Update metrics, trigger calculations, generate reports, etc.

            // Mark as processed in inbox
            InboxMessage inboxMessage = InboxMessage.create(
                messageId,
                event.getClass().getSimpleName(),
                event.getCorrelationId(),
                Instant.now()
            );
            inboxRepository.save(inboxMessage);

            logger.info("Successfully processed DataQualityCompletedInboundEvent for batch {}", 
                event.getBatchId());

        } catch (Exception ex) {
            logger.error("Failed to process DataQualityCompletedInboundEvent for batch {}", 
                event.getBatchId(), ex);
            throw ex;
        }
    }

    private String generateMessageId(DataQualityCompletedInboundEvent event) {
        return String.format("data-quality-completed-%s", event.getBatchId());
    }
}
```

**Key Points:**
- ✅ Use **@Transactional** to ensure atomicity
- ✅ Check **inbox** for idempotency (prevent duplicate processing)
- ✅ Perform business logic
- ✅ Save to **inbox** after successful processing
- ✅ Use unique message ID for idempotency check

## Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       PUBLISHER MODULE (Data Quality)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Domain Event Raised                                                     │
│     QualityValidationCompletedEvent (domain layer)                          │
│                    ↓                                                         │
│  2. Event Publisher Listens (@EventListener)                                │
│     BatchQualityCompletedEventPublisher (infrastructure layer)              │
│                    ↓                                                         │
│  3. Convert to Integration Event                                            │
│     DataQualityCompletedIntegrationEvent (core module)                      │
│                    ↓                                                         │
│  4. Publish via IIntegrationEventBus                                        │
│     CrossModuleEventBus.publish()                                           │
│                    ↓                                                         │
│  5. Persist to Outbox (Transactional)                                       │
│     outbox_messages table                                                   │
│                                                                              │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │
                     ┌─────────────▼──────────────┐
                     │  OUTBOX PROCESSOR          │
                     │  (Background Job)          │
                     │  - Reads pending messages  │
                     │  - Publishes to Spring     │
                     │  - Marks as published      │
                     └─────────────┬──────────────┘
                                   │
┌──────────────────────────────────▼───────────────────────────────────────────┐
│                       CONSUMER MODULE (Metrics/Reports)                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Spring Event Bus Delivers Event                                         │
│     ApplicationEventPublisher → DataQualityCompletedInboundEvent            │
│                    ↓                                                         │
│  2. Event Listener Receives (@EventListener)                                │
│     DataQualityCompletedIntegrationEventListener (presentation layer)       │
│                    ↓                                                         │
│  3. Delegate to Use Case                                                    │
│     UpdateDashboardMetricsOnDataQualityCompletedUseCase (application)       │
│                    ↓                                                         │
│  4. Check Inbox for Idempotency                                             │
│     inboxRepository.existsByMessageId()                                     │
│                    ↓                                                         │
│  5. Process Business Logic (if not already processed)                       │
│     metricsService.updateQualityMetrics()                                   │
│                    ↓                                                         │
│  6. Save to Inbox (Transactional)                                           │
│     inbox_messages table                                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Checklist for Adding New Cross-Module Event

### Publishing Side (Producer Module)

- [ ] Create `{EventName}IntegrationEvent` in `regtech-core/domain/events/integration/`
  - [ ] Extends `IntegrationEvent`
  - [ ] All fields are `final`
  - [ ] Uses `@JsonCreator` and `@JsonProperty`
  - [ ] Includes `correlationId`
  
- [ ] Create `{EventName}InboundEvent` in `regtech-core/domain/events/integration/`
  - [ ] Extends `DomainEvent`
  - [ ] Same fields as IntegrationEvent version
  - [ ] Uses `@JsonCreator` and `@JsonProperty`
  - [ ] Optional: Add `isValid()` method
  
- [ ] Create `{EventName}Publisher` in `{module}/infrastructure/messaging/outbound/`
  - [ ] Annotated with `@Component` (unique bean name)
  - [ ] Injects `IIntegrationEventBus`
  - [ ] Listens to domain event with `@EventListener`
  - [ ] Checks `CorrelationContext.isOutboxReplay()` to prevent duplicates
  - [ ] Converts domain event → integration event
  - [ ] Sets `OUTBOX_REPLAY = true` when publishing
  - [ ] Logs success/failure

### Consuming Side (Consumer Module)

- [ ] Create `{EventName}IntegrationEventListener` in `{module}/presentation/integration/listener/`
  - [ ] Annotated with `@Component` (unique bean name)
  - [ ] Listens to `{EventName}InboundEvent` with `@EventListener`
  - [ ] Checks `CorrelationContext.isInboxReplay()` to prevent duplicates
  - [ ] Sets `INBOX_REPLAY = true` when calling use case
  - [ ] Delegates to application layer use case
  
- [ ] Create use case in `{module}/application/`
  - [ ] Annotated with `@Service`
  - [ ] Method annotated with `@Transactional`
  - [ ] Injects `IInboxMessageRepository`
  - [ ] Checks inbox for idempotency
  - [ ] Performs business logic
  - [ ] Saves to inbox after successful processing
  - [ ] Generates unique message ID

## Common Pitfalls & Solutions

### ❌ Problem: Events Published Twice

**Cause**: Forgetting to check `CorrelationContext.isOutboxReplay()`

**Solution**:
```java
@EventListener
public void handle(DomainEvent event) {
    if (CorrelationContext.isOutboxReplay()) {
        return; // Skip republishing
    }
    // ... publish integration event
}
```

### ❌ Problem: Events Processed Twice

**Cause**: Not checking inbox for idempotency

**Solution**:
```java
@Transactional
public void process(InboundEvent event) {
    String messageId = generateMessageId(event);
    if (inboxRepository.existsByMessageId(messageId)) {
        return; // Already processed
    }
    // ... process event
    inboxRepository.save(new InboxMessage(messageId, ...));
}
```

### ❌ Problem: Missing Correlation Context

**Cause**: Not using `ScopedValue` to propagate context

**Solution**:
```java
ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
        .where(CorrelationContext.OUTBOX_REPLAY, true)
        .run(() -> eventBus.publish(integrationEvent));
```

### ❌ Problem: Transaction Rollback Loses Events

**Cause**: Publishing outside transaction or not using outbox pattern

**Solution**: The `CrossModuleEventBus` automatically handles this by:
- Detecting active transactions
- Persisting to outbox table within transaction
- Background processor publishes after commit

### ❌ Problem: JSON Serialization Errors

**Cause**: Missing `@JsonCreator` or `@JsonProperty` annotations

**Solution**:
```java
@JsonCreator
public MyEvent(@JsonProperty("field1") String field1, 
               @JsonProperty("field2") int field2) {
    // ...
}
```

## Testing Integration Events

### Unit Test: Publisher

```java
@ExtendWith(MockitoExtension.class)
class BatchQualityCompletedEventPublisherTest {

    @Mock
    private IIntegrationEventBus eventBus;
    
    @InjectMocks
    private BatchQualityCompletedEventPublisher publisher;

    @Test
    void shouldPublishIntegrationEventWhenDomainEventReceived() {
        // Arrange
        QualityValidationCompletedEvent domainEvent = createDomainEvent();
        
        // Act
        publisher.handle(domainEvent);
        
        // Assert
        verify(eventBus).publish(argThat(event -> 
            event instanceof DataQualityCompletedIntegrationEvent &&
            ((DataQualityCompletedIntegrationEvent) event).getBatchId().equals("batch-123")
        ));
    }
    
    @Test
    void shouldSkipPublishingDuringOutboxReplay() {
        // Arrange
        QualityValidationCompletedEvent domainEvent = createDomainEvent();
        
        // Act
        ScopedValue.where(CorrelationContext.OUTBOX_REPLAY, true)
                .run(() -> publisher.handle(domainEvent));
        
        // Assert
        verify(eventBus, never()).publish(any());
    }
}
```

### Integration Test: End-to-End Flow

```java
@SpringBootTest
@Transactional
class CrossModuleEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private IOutboxMessageRepository outboxRepository;
    
    @Autowired
    private IInboxMessageRepository inboxRepository;

    @Test
    void shouldPublishAndConsumeEventAcrossModules() {
        // 1. Publish domain event
        QualityValidationCompletedEvent domainEvent = createDomainEvent();
        eventPublisher.publishEvent(domainEvent);
        
        // 2. Verify outbox contains message
        List<OutboxMessage> outboxMessages = outboxRepository.findPending();
        assertThat(outboxMessages).hasSize(1);
        assertThat(outboxMessages.get(0).getType())
                .contains("DataQualityCompletedIntegrationEvent");
        
        // 3. Simulate outbox processor
        OutboxProcessor processor = new OutboxProcessor(outboxRepository, eventPublisher);
        processor.processMessages();
        
        // 4. Verify inbox contains processed message
        String messageId = "data-quality-completed-batch-123";
        assertThat(inboxRepository.existsByMessageId(messageId)).isTrue();
    }
}
```

## Configuration

### application.yml

```yaml
# Outbox processing configuration
regtech:
  outbox:
    enabled: true
    processing-interval: 30000  # 30 seconds
    retry-interval: 60000  # 1 minute
    max-retries: 3
    
  inbox:
    enabled: true
    processing-interval: 10000  # 10 seconds
    batch-size: 20

# Cross-module event bus configuration
events:
  cross-module:
    max-concurrent-publishes: 50  # Virtual thread capacity
    acquire-timeout-ms: 2000  # Timeout before fallback to sync
```

## Monitoring & Observability

### Key Metrics to Monitor

1. **Outbox Messages**:
   - Pending count: `SELECT COUNT(*) FROM outbox_messages WHERE status = 'PENDING'`
   - Failed count: `SELECT COUNT(*) FROM outbox_messages WHERE status = 'FAILED'`
   - Average processing time
   
2. **Inbox Messages**:
   - Total processed: `SELECT COUNT(*) FROM inbox_messages`
   - Duplicate attempts (idempotency hits)
   
3. **Event Bus**:
   - Published events per second
   - Failed publishes
   - Average publish latency

### Logging

Publishers and consumers automatically log:
- ✅ Event published successfully
- ❌ Event publish failed
- ⏭️ Event skipped (replay detected)
- ✅ Event processed successfully
- ❌ Event processing failed
- 🔄 Duplicate event detected (idempotency)

## Summary

### Key Takeaways

1. **Two Event Types**: `IntegrationEvent` (outbound) and `InboundEvent` (inbound)
2. **Location**: Both defined in `regtech-core/domain/events/integration/`
3. **Publisher**: Infrastructure layer, listens to domain events
4. **Consumer**: Presentation layer, delegates to application layer
5. **Patterns**: Outbox (publishing), Inbox (consuming)
6. **Idempotency**: Publishers check outbox replay, consumers check inbox
7. **Transaction Safety**: Outbox ensures events survive rollbacks
8. **Correlation**: Always propagate correlation ID via `ScopedValue`

### Architecture Benefits

✅ **Loose Coupling**: Modules don't directly depend on each other  
✅ **Reliability**: Outbox/Inbox patterns ensure delivery  
✅ **Idempotency**: Prevents duplicate processing  
✅ **Traceability**: Correlation IDs enable end-to-end tracking  
✅ **Scalability**: Virtual threads handle high event volumes  
✅ **Consistency**: Transactional guarantees within modules  

---

**Next Steps**: When implementing a new cross-module integration, follow the checklist above and refer to this guide for patterns and best practices.
