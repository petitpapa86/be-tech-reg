# Bounded Context Implementation Checklist

Use this checklist when implementing a new bounded context to ensure consistency with the established architecture patterns.

## Pre-Implementation Planning

### Domain Analysis
- [ ] Identify the bounded context boundaries
- [ ] Define the ubiquitous language for the context
- [ ] Identify aggregate roots and their boundaries
- [ ] Map out domain events and their flows
- [ ] Define value objects and entities
- [ ] Identify cross-context integration points

### Technical Planning
- [ ] Choose the context name (lowercase, singular)
- [ ] Plan the Maven module structure
- [ ] Identify external dependencies
- [ ] Plan database schema and migrations
- [ ] Define API endpoints and contracts

## Project Setup

### Maven Module
- [ ] Create `regtech-{context}` directory
- [ ] Set up `pom.xml` with proper dependencies
- [ ] Configure parent POM reference
- [ ] Add module to root POM
- [ ] Create basic directory structure

### Configuration
- [ ] Create `{Context}Module.java` Spring Boot module class
- [ ] Set up `application-{context}.yml`
- [ ] Configure database connection properties
- [ ] Set up test configuration `application-test.yml`

## Domain Layer Implementation

### Directory Structure
- [ ] Create `domain/` package
- [ ] Create concept-based packages (e.g., `users/`, `documents/`)
- [ ] Create `repositories/` package for interfaces
- [ ] Create `services/` package for domain services
- [ ] Create `shared/` package for cross-cutting concerns

### Aggregate Implementation
- [ ] Implement aggregate root classes
  - [ ] Private constructors
  - [ ] Factory methods with validation
  - [ ] Business methods returning `Result<T>`
  - [ ] Proper encapsulation
  - [ ] Version field for optimistic locking
- [ ] Implement value objects as records
  - [ ] Validation in constructors
  - [ ] Factory methods
  - [ ] Proper equals/hashCode
- [ ] Define domain events extending `BaseEvent`
- [ ] Create repository interfaces with functional methods

### Domain Services (Optional)
- [ ] Implement domain services for complex business logic (if needed)
- [ ] Ensure services are stateless
- [ ] Keep domain layer free of infrastructure dependencies

## Application Layer Implementation

### Directory Structure
- [ ] Create `application/` package
- [ ] Create action-based packages (e.g., `createuser/`, `updateuser/`)
- [ ] Create `events/` package for cross-cutting event handlers
- [ ] Create `sagas/` package for long-running processes
- [ ] Create `shared/` package for shared DTOs

### Command/Query Implementation
- [ ] Implement commands as immutable records
  - [ ] Validation annotations
  - [ ] Factory methods with validation
  - [ ] Proper naming (`{Action}{Entity}Command`)
- [ ] Implement command handlers
  - [ ] Inject closure-based repositories
  - [ ] Use pure static functions for business logic
  - [ ] Pass closures as function parameters for testability
  - [ ] Proper error handling with Result types
  - [ ] Event publishing
- [ ] Implement response DTOs
  - [ ] Factory methods
  - [ ] Proper naming (`{Action}{Entity}Response`)

### Event Handlers
- [ ] Implement cross-context event handlers
- [ ] Use `@EventListener` annotation
- [ ] Handle correlation IDs properly
- [ ] Implement proper error handling

### Sagas (if needed)
- [ ] Implement saga classes extending `Saga<T>`
- [ ] Create saga data classes extending `SagaData`
- [ ] Implement step-by-step execution logic
- [ ] Add compensation logic for failures

## Infrastructure Layer Implementation

### Directory Structure
- [ ] Create `infrastructure/` package
- [ ] Create `configuration/` for Spring configs
- [ ] Create `database/` for persistence
- [ ] Create `external/` for external integrations
- [ ] Create `events/` for event infrastructure
- [ ] Create `jobs/` for scheduled tasks
- [ ] Create `monitoring/` for observability

### Database Implementation
- [ ] Create JPA entities in `database/entities/`
  - [ ] Proper JPA annotations
  - [ ] Conversion methods to/from domain objects (`fromDomain()`, `toDomain()`)
  - [ ] Optimistic locking with `@Version`
- [ ] Implement closure-based repository classes in `database/repositories/`
  - [ ] Use `@Repository` and `@Transactional` annotations
  - [ ] Provide functional closures (e.g., `userFinder()`, `userSaver()`)
  - [ ] Use EntityManager for complex queries
  - [ ] Proper error handling with Result/Maybe types
- [ ] Create database migrations in `resources/db/migration/`
  - [ ] Follow Flyway naming conventions
  - [ ] Include proper indexes
  - [ ] Add constraints and foreign keys

### External Integrations
- [ ] Create client classes for external services
- [ ] Implement proper error handling and retries
- [ ] Use configuration properties for URLs and credentials
- [ ] Add circuit breaker patterns if needed

### Event Infrastructure
- [ ] Implement event publisher classes
- [ ] Set up outbox pattern for reliable event publishing
- [ ] Configure event serialization/deserialization

### Monitoring and Observability
- [ ] Add metrics collection
- [ ] Implement health checks
- [ ] Add structured logging
- [ ] Set up distributed tracing

## API Layer Implementation

### Directory Structure
- [ ] Create `api/` package
- [ ] Create entity-based packages (e.g., `users/`, `documents/`)
- [ ] Create `monitoring/` for admin endpoints
- [ ] Create `webhooks/` for external webhooks

### Controller Implementation
- [ ] Implement REST controllers
  - [ ] Proper HTTP methods and status codes
  - [ ] Request/response DTOs
  - [ ] Validation annotations
  - [ ] Error handling
- [ ] Extend `BaseController` for common functionality
- [ ] Use `ApiResponse<T>` wrapper for responses
- [ ] Implement proper security annotations

### API Documentation
- [ ] Add OpenAPI/Swagger annotations
- [ ] Document all endpoints
- [ ] Provide example requests/responses
- [ ] Document error codes and messages

## Testing Implementation

### Test Structure
- [ ] Create test packages mirroring main packages
- [ ] Set up test configuration
- [ ] Create test data builders/factories

### Unit Tests
- [ ] Test domain logic (aggregates, value objects)
- [ ] Test application services (command handlers)
- [ ] Test repository implementations
- [ ] Achieve >80% code coverage

### Integration Tests
- [ ] Test database operations
- [ ] Test external service integrations
- [ ] Test event publishing/handling
- [ ] Use test containers for database tests

### API Tests
- [ ] Test controller endpoints
- [ ] Test request/response serialization
- [ ] Test error handling
- [ ] Test security configurations

## Cross-Context Integration

### Event Integration
- [ ] Define events published by this context
- [ ] Implement event handlers for consumed events
- [ ] Set up proper correlation ID handling
- [ ] Test event flows end-to-end

### Shared Kernel
- [ ] Add shared events to `regtech-core`
- [ ] Update shared value objects if needed
- [ ] Ensure backward compatibility

## Documentation

### Code Documentation
- [ ] Add JavaDoc to public APIs
- [ ] Document complex business logic
- [ ] Add inline comments for non-obvious code

### Architecture Documentation
- [ ] Create context README.md
- [ ] Document domain model
- [ ] Document API contracts
- [ ] Document integration points

### Operational Documentation
- [ ] Document deployment procedures
- [ ] Document monitoring and alerting
- [ ] Document troubleshooting guides

## Quality Assurance

### Code Quality
- [ ] Run static analysis tools
- [ ] Ensure consistent formatting
- [ ] Review code for security issues
- [ ] Check for proper error handling

### Performance
- [ ] Profile critical paths
- [ ] Optimize database queries
- [ ] Add appropriate caching
- [ ] Load test critical endpoints

### Security
- [ ] Implement proper authentication/authorization
- [ ] Validate all inputs
- [ ] Protect against common vulnerabilities
- [ ] Review sensitive data handling

## Deployment Preparation

### Configuration
- [ ] Set up environment-specific configurations
- [ ] Configure secrets management
- [ ] Set up monitoring and alerting
- [ ] Configure log aggregation

### Database
- [ ] Test database migrations
- [ ] Set up backup procedures
- [ ] Configure connection pooling
- [ ] Set up read replicas if needed

### Monitoring
- [ ] Set up application metrics
- [ ] Configure health checks
- [ ] Set up alerting rules
- [ ] Test monitoring dashboards

## Post-Implementation

### Validation
- [ ] Verify all functionality works as expected
- [ ] Test integration with other contexts
- [ ] Validate performance requirements
- [ ] Confirm security requirements

### Documentation Updates
- [ ] Update architecture diagrams
- [ ] Update API documentation
- [ ] Update deployment guides
- [ ] Update troubleshooting documentation

### Knowledge Transfer
- [ ] Conduct code reviews
- [ ] Share implementation patterns
- [ ] Document lessons learned
- [ ] Update team knowledge base

## Maintenance Checklist

### Regular Tasks
- [ ] Monitor application metrics
- [ ] Review and update dependencies
- [ ] Optimize performance bottlenecks
- [ ] Update documentation as needed

### Periodic Reviews
- [ ] Review domain model for changes
- [ ] Assess integration patterns
- [ ] Evaluate technical debt
- [ ] Plan refactoring initiatives

This checklist ensures that all bounded contexts follow the same high-quality standards and architectural patterns established in the RegTech system.