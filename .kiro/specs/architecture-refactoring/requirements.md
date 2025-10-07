# Architecture Refactoring Requirements

## 📋 **MANDATORY Architecture Guide Reference**
**ALL refactoring work MUST follow the patterns and principles defined in:**
📋 **[Functional Bounded Context Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)**

This comprehensive guide contains:
- **Handler Pattern Standards**: Mandatory functional composition with `{Capability}RepositoryFunctions`
- **Repository Function Interfaces**: Functional interfaces wrapping concrete repositories
- **Command/Query/Response Structures**: Immutable records with validation
- **Error Handling Standards**: Consistent error types and Result pattern
- **Package Organization Rules**: Capability-based structure for all bounded contexts
- **Testing Patterns**: Complete testing approaches and examples
- **Validation Standards**: Bean validation and custom validation patterns
- **API Layer Patterns**: REST controller implementations
- **Infrastructure Patterns**: Repository implementations and health checks
- **Logging and Metrics**: Structured logging and observability patterns

## Introduction

This specification defines the requirements for refactoring the current BCBS239 compliance platform from a technical-concern-based organization to a capability-based organization within each bounded context. The refactoring will implement proper DDD vertical slices with aggregate boundaries while maintaining the modular monolith structure.

**CRITICAL**: All implementations must strictly follow the Architecture Guide to prevent future refactoring cycles and ensure architectural consistency.

## Requirements

### Requirement 1: Domain Layer Capability Organization

**User Story:** As a developer, I want the domain layer organized by business capabilities rather than technical concerns, so that related domain logic is co-located and easier to understand.

#### Acceptance Criteria

1. WHEN organizing domain packages THEN the system SHALL group domain types by business capabilities in sub-aggregates
2. WHEN creating domain structure THEN each bounded context SHALL have domain packages organized as `/domain/{capability}/` where capability represents a business aggregate
3. WHEN implementing domain logic THEN each capability SHALL contain its own models, events, services, and repositories interfaces
4. WHEN defining aggregates THEN each capability SHALL represent a proper DDD aggregate boundary with clear consistency rules
5. WHEN referencing other aggregates THEN capabilities SHALL reference other aggregates by ID only, never by direct object reference
6. WHEN implementing cross-aggregate relationships THEN the system SHALL use aggregate IDs to maintain loose coupling and enable future service extraction

### Requirement 2: Infrastructure Layer Capability Mirroring

**User Story:** As a developer, I want infrastructure components organized to mirror domain capabilities, so that persistence and external integrations are clearly mapped to business capabilities.

#### Acceptance Criteria

1. WHEN organizing infrastructure packages THEN the system SHALL mirror domain structure with `/infrastructure/{capability}/` packages
2. WHEN implementing persistence THEN each capability SHALL have its own repository implementations, entities, and database migrations
3. WHEN integrating external systems THEN each capability SHALL have its own adapters and client implementations
4. WHEN providing observability THEN each capability SHALL have its own metrics, health checks, and monitoring components

### Requirement 3: Application Layer Use Case Organization

**User Story:** As a developer, I want application services organized by business use cases, so that business workflows are clearly defined and testable.

#### Acceptance Criteria

1. WHEN organizing application packages THEN the system SHALL group services by business use cases in `/application/{capability}/`
2. WHEN implementing use cases THEN each capability SHALL contain application services, handlers, and command/query objects
3. WHEN defining workflows THEN each use case SHALL be implemented as pure business functions with clear inputs and outputs
4. WHEN composing operations THEN handler classes SHALL compose domain services using functional closures

### Requirement 4: Presentation Layer Capability Endpoints

**User Story:** As a developer, I want API endpoints organized by business capabilities, so that external interfaces clearly represent business operations.

#### Acceptance Criteria

1. WHEN organizing presentation packages THEN the system SHALL group controllers by business capabilities in `/api/{capability}/` or `/presentation/{capability}/`
2. WHEN implementing endpoints THEN each capability SHALL have its own controllers, DTOs, and validation logic
3. WHEN exposing APIs THEN each capability SHALL provide clear business-focused endpoints
4. WHEN handling requests THEN controllers SHALL delegate to appropriate application services for the capability

### Requirement 5: Database Schema Capability Isolation

**User Story:** As a system administrator, I want database schemas organized by business capabilities, so that data isolation and evolution can be managed independently.

#### Acceptance Criteria

1. WHEN organizing database schemas THEN each capability SHALL operate within its own dedicated PostgreSQL schema
2. WHEN implementing persistence THEN each capability SHALL have complete data isolation from other capabilities
3. WHEN evolving schemas THEN each capability SHALL be able to evolve its data model independently
4. WHEN accessing data THEN cross-capability data access SHALL only occur through well-defined domain events or APIs

### Requirement 6: Capability Coverage Completeness

**User Story:** As a developer, I want complete infrastructure coverage for all domain capabilities, so that every business capability has proper persistence and external integration support.

#### Acceptance Criteria

1. WHEN implementing capabilities THEN infrastructure components SHALL be organized to reflect all domain entities and aggregates
2. WHEN providing persistence THEN each domain aggregate SHALL have corresponding repository implementations and database tables
3. WHEN integrating externally THEN each capability requiring external integration SHALL have dedicated adapter implementations
4. WHEN monitoring systems THEN each capability SHALL have comprehensive observability coverage

### Requirement 7: Aggregate Reference Isolation

**User Story:** As a system architect, I want aggregates to reference each other only by ID, so that capabilities can be extracted into separate services in the future without breaking dependencies.

#### Acceptance Criteria

1. WHEN one aggregate references another THEN it SHALL use only the aggregate's ID, never direct object references
2. WHEN implementing cross-aggregate operations THEN the system SHALL load aggregates separately using repository lookups by ID
3. WHEN designing domain events THEN events SHALL contain aggregate IDs rather than full aggregate objects
4. WHEN implementing application services THEN they SHALL orchestrate multiple aggregates by loading them via their IDs
5. WHEN persisting aggregate relationships THEN foreign keys SHALL reference aggregate root IDs only
6. WHEN querying across aggregates THEN the system SHALL use eventual consistency patterns rather than direct joins

## Identified Capabilities for Refactoring

Based on the current codebase analysis, the following capabilities have been identified for each bounded context:

### IAM Bounded Context Capabilities
- **User Management** - User registration, profile management, status handling
- **Authentication** - Password-based and OAuth2 authentication flows
- **Session Management** - User session lifecycle and token management
- **Authorization** - Role-based access control and bank permissions
- **Security** - Security events, audit logging, and threat detection

### Billing Bounded Context Capabilities
- **Account Management** - Billing account lifecycle and configuration
- **Usage Tracking** - Usage metrics collection and analysis
- **Invoice Management** - Invoice generation, distribution, and lifecycle
- **Payment Processing** - Payment collection, retry logic, and failure handling
- **Subscription Management** - Tier management and subscription lifecycle
- **Tax Compliance** - Tax calculation, reporting, and compliance validation
- **Dunning Management** - Payment recovery and service restriction workflows

### Core Capabilities (Cross-cutting)
- **Event Management** - Cross-module event publishing and handling
- **Composition** - Service composition and orchestration
- **Health Monitoring** - System health checks and monitoring
- **Configuration** - Module configuration and properties management

## Success Criteria

1. **Clear Capability Boundaries**: Each business capability is clearly defined with its own package structure
2. **Complete Infrastructure Coverage**: Every domain capability has corresponding infrastructure implementations
3. **Independent Evolution**: Each capability can evolve its implementation independently
4. **Maintainable Codebase**: Related functionality is co-located and easy to find
5. **Testable Architecture**: Each capability can be tested in isolation with clear boundaries
6. **Schema Isolation**: Each capability operates within its own database schema
7. **Proper DDD Implementation**: Aggregate boundaries are clearly defined and enforced