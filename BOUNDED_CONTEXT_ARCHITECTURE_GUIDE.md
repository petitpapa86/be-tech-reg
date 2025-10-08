# Bounded Context Architecture Guide

This guide defines the standard architecture patterns and organizational structure to follow when implementing bounded contexts in the RegTech system. It's based on the patterns established in the billing context and follows Domain-Driven Design (DDD) principles.

## Table of Contents

1. [Overview](#overview)
2. [Directory Structure](#directory-structure)
3. [Domain Layer Organization](#domain-layer-organization)
4. [Application Layer Organization](#application-layer-organization)
5. [Infrastructure Layer Organization](#infrastructure-layer-organization)
6. [API Layer Organization](#api-layer-organization)
7. [Testing Structure](#testing-structure)
8. [Naming Conventions](#naming-conventions)
9. [Cross-Context Integration](#cross-context-integration)
10. [Implementation Checklist](#implementation-checklist)

## Overview

Each bounded context should be implemented as a separate Maven module following a consistent layered architecture:

- **Domain Layer**: Core business logic, entities, value objects, and domain events
- **Application Layer**: Use cases, command handlers, and application services
- **Infrastructure Layer**: External integrations, persistence, and technical concerns
- **API Layer**: REST controllers and external interfaces

## Directory Structure

```
regtech-{context}/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/bcbs239/regtech/{context}/
│   │   │   ├── {Context}Module.java
│   │   │   ├── api/
│   │   │   ├── application/
│   │   │   ├── domain/
│   │   │   └── infrastructure/
│   │   └── resources/
│   │       ├── application-{context}.yml
│   │       └── db/migration/
│   └── test/
│       ├── java/com/bcbs239/regtech/{context}/
│       └── resources/
│           └── application-test.yml
```

## Domain Layer Organization

The domain layer should be organized by aggregate roots and domain concepts:

```
domain/
├── {concept1}/          # Domain concept (e.g., users, subscriptions, invoices)
│   ├── {Aggregate}.java
│   ├── {ConceptEvent}.java
│   ├── {ConceptId}.java
│   ├── {ConceptStatus}.java
│   └── ...
├── {concept2}/
├── events/              # Cross-cutting domain events (if any)
├── services/           # Domain services (if needed)
└── valueobjects/       # Cross-cutting value objects
```

### Domain Layer Guidelines

1. **Concept-Based Organization**: Group related domain objects by business concept
   ```java
   // Example: User concept
   domain/users/
   ├── User.java              // Aggregate root
   ├── UserRegisteredEvent.java
   ├── UserId.java
   ├── UserStatus.java
   ├── UserProfile.java
   └── UserRepository.java    // Repository interface
   ```

2. **Aggregate Rules**:
   - Each aggregate should have a single root entity
   - Keep aggregates small and focused
   - Use value objects for immutable data
   - Publish domain events for significant business events

3. **Value Objects**:
   - Use records for simple value objects
   - Include validation in constructors
   - Provide factory methods for complex creation logic

## Application Layer Organization

The application layer should be organized by business actions/use cases:

```
application/
├── {action1}{entity}/    # e.g., createuser, registeruser
│   ├── {Action}{Entity}Command.java
│   ├── {Action}{Entity}CommandHandler.java
│   ├── {Action}{Entity}Response.java
│   └── {RelatedEventHandler}.java (if applicable)
├── {action2}{entity}/
├── events/              # Cross-cutting event handlers
├── sagas/              # Long-running business processes
│   ├── {Process}Saga.java
│   └── {Process}SagaData.java
└── shared/             # Shared DTOs and utilities
    ├── {SharedDto}.java
    └── {SharedValueObject}.java
```

### Application Layer Guidelines

1. **Action-Based Organization**: Each business action gets its own package
   ```java
   // Example: User registration
   application/registeruser/
   ├── RegisterUserCommand.java
   ├── RegisterUserCommandHandler.java
   ├── RegisterUserResponse.java
   └── UserRegistrationEventHandler.java
   ```

2. **Command Pattern**:
   - Commands are immutable data structures
   - Command handlers contain the business logic
   - Use functional programming patterns where appropriate
   - Return Result<T> for error handling

3. **Event Handlers**:
   - Place event handlers in the action package they belong to
   - Use cross-cutting event handlers for system-wide concerns

## Infrastructure Layer Organization

```
infrastructure/
├── configuration/       # Spring configuration classes
├── database/
│   ├── entities/       # JPA entities
│   └── repositories/   # JPA repository implementations (closure-based)
├── events/             # Event publishing infrastructure
├── external/           # External service integrations
│   ├── {service}/
│   │   ├── {Service}Client.java
│   │   ├── {Service}Configuration.java
│   │   └── dto/
├── jobs/              # Scheduled jobs and background tasks
├── messaging/         # Message queue integrations
├── observability/     # Metrics, logging, monitoring
└── web/              # Web-specific configurations
```

### Infrastructure Guidelines

1. **Closure-Based Repositories**: Use functional closures instead of traditional repository interfaces
2. **External Integrations**: Each external service gets its own package
3. **Database**: Separate entities from domain objects with conversion methods
4. **Configuration**: Use type-safe configuration properties
5. **Observability**: Include metrics, audit logging, and monitoring

## API Layer Organization

```
api/
├── {entity1}/          # e.g., users, billing, subscriptions
│   └── {Entity}Controller.java
├── {entity2}/
├── monitoring/         # Health checks, metrics endpoints
│   └── {Context}MonitoringController.java
├── scheduling/         # Administrative endpoints
│   └── {Context}SchedulingController.java
└── webhooks/          # External webhook endpoints
    └── WebhookController.java
```

### API Guidelines

1. **Entity-Based Controllers**: Group endpoints by business entity
2. **RESTful Design**: Follow REST conventions
3. **Error Handling**: Use consistent error response format
4. **Validation**: Use Bean Validation annotations

## Testing Structure

```
test/
├── java/com/bcbs239/regtech/{context}/
│   ├── api/                    # Controller tests
│   ├── application/            # Application service tests
│   ├── domain/                 # Domain logic tests
│   └── infrastructure/         # Infrastructure tests
└── resources/
    ├── application-test.yml
    └── test-data/
```

### Testing Guidelines

1. **Unit Tests**: Test domain logic and application services
2. **Integration Tests**: Test infrastructure components
3. **API Tests**: Test controller endpoints
4. **Test Data**: Use builders and factories for test data

## Naming Conventions

### Packages
- Use lowercase, singular nouns: `user`, `billing`, `invoice`
- Action packages: `{verb}{noun}` (e.g., `createuser`, `cancelsubscription`)

### Classes
- **Commands**: `{Action}{Entity}Command` (e.g., `RegisterUserCommand`)
- **Handlers**: `{Action}{Entity}CommandHandler`
- **Responses**: `{Action}{Entity}Response`
- **Events**: `{Entity}{Action}Event` (e.g., `UserRegisteredEvent`)
- **Aggregates**: `{Entity}` (e.g., `User`, `Subscription`)
- **Value Objects**: `{Entity}{Property}` (e.g., `UserId`, `UserStatus`)

### Methods
- **Commands**: Use imperative verbs (`register`, `cancel`, `update`)
- **Queries**: Use descriptive names (`findActiveUsers`, `getUserById`)
- **Domain Methods**: Use business language (`activate`, `suspend`, `process`)

## Cross-Context Integration

### Event-Driven Communication
```java
// Publishing events
@Component
public class UserRegistrationHandler {
    private final EventPublisher eventPublisher;
    
    public Result<User> handle(RegisterUserCommand command) {
        // ... business logic
        eventPublisher.publish(new UserRegisteredEvent(user.getId(), correlationId));
        return Result.success(user);
    }
}

// Consuming events
@Component
public class UserRegisteredEventHandler {
    @EventHandler
    public void handle(UserRegisteredEvent event) {
        // Handle cross-context integration
    }
}
```

### Shared Kernel
- Place shared concepts in `regtech-core`
- Include common value objects, events, and utilities
- Avoid sharing domain logic between contexts

## Implementation Checklist

### Domain Layer
- [ ] Identify aggregates and their boundaries
- [ ] Define value objects and entities
- [ ] Create domain events for significant business events
- [ ] Implement repository interfaces
- [ ] Add domain services for complex business logic

### Application Layer
- [ ] Define commands for each use case
- [ ] Implement command handlers with business logic
- [ ] Create response DTOs
- [ ] Add event handlers for cross-context integration
- [ ] Implement sagas for long-running processes

### Infrastructure Layer
- [ ] Create JPA entities and repositories
- [ ] Configure external service clients
- [ ] Set up event publishing infrastructure
- [ ] Add monitoring and metrics
- [ ] Configure database migrations

### API Layer
- [ ] Create REST controllers
- [ ] Add request/response DTOs
- [ ] Implement error handling
- [ ] Add API documentation
- [ ] Configure security

### Testing
- [ ] Write unit tests for domain logic
- [ ] Add integration tests for repositories
- [ ] Create API tests for controllers
- [ ] Add performance tests for critical paths

## Example Implementation

Here's how to implement a new "Document Processing" bounded context:

```java
// Domain
regtech-document/src/main/java/com/bcbs239/regtech/document/
├── domain/
│   ├── documents/
│   │   ├── Document.java
│   │   ├── DocumentProcessedEvent.java
│   │   ├── DocumentId.java
│   │   └── DocumentStatus.java
│   └── processing/
│       ├── ProcessingJob.java
│       └── ProcessingJobId.java

// Application
├── application/
│   ├── processdocument/
│   │   ├── ProcessDocumentCommand.java
│   │   ├── ProcessDocumentCommandHandler.java
│   │   └── ProcessDocumentResponse.java
│   └── shared/
│       └── ProcessingMetrics.java

// Infrastructure
├── infrastructure/
│   ├── database/
│   │   ├── entities/
│   │   └── repositories/
│   └── external/
│       └── ocr/
│           └── OcrServiceClient.java

// API
└── api/
    └── documents/
        └── DocumentController.java
```

This architecture ensures consistency across bounded contexts while maintaining clear separation of concerns and enabling independent development and deployment.