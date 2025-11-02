# Modular Monolith Architecture Guide

## Overview

This document defines the standardized modular monolith architecture for the RegTech application. This architecture provides clear separation of concerns, maintainable code organization, and scalable module structure while maintaining the benefits of a monolithic deployment model.

## Architecture Principles

### 1. **Modular Design**
- Each business domain is organized as a separate module
- Modules are self-contained with clear boundaries
- Inter-module communication follows defined patterns

### 2. **Layered Architecture**
- Each module follows a 4-layer architecture pattern
- Clear dependency direction (outer layers depend on inner layers)
- Domain layer remains pure and framework-agnostic

### 3. **Separation of Concerns**
- Each layer has specific responsibilities
- Infrastructure concerns are isolated from business logic
- Presentation logic is separated from application logic

## Module Structure

Each module follows this standardized directory structure:

```
{module-name}/
├── domain/                     # Core business logic
├── application/               # Use cases and orchestration
├── infrastructure/           # External concerns and implementations
├── presentation/            # API endpoints and web concerns
└── pom.xml                 # Module parent POM
```

### Package Naming Convention

```
com.bcbs239.regtech.modules.{module-name}.{layer}.{concern}
```

**Examples:**
- `com.bcbs239.regtech.modules.ingestion.domain.batch`
- `com.bcbs239.regtech.modules.ingestion.application.batch.commands`
- `com.bcbs239.regtech.modules.ingestion.infrastructure.security`
- `com.bcbs239.regtech.modules.ingestion.presentation.batch.upload`

## Layer Definitions

### 1. Domain Layer (`domain/`)

**Purpose:** Contains pure business logic, entities, and domain services.

**Structure:**
```
domain/src/main/java/.../domain/
├── {entity}/                  # Business entities (e.g., batch, bankinfo)
│   ├── {Entity}.java         # Domain entity
│   ├── {Entity}Id.java       # Value object for entity ID
│   ├── I{Entity}Repository.java # Repository interface
│   └── {Entity}*Event.java   # Domain events
├── services/                 # Domain service interfaces
├── integrationevents/       # Cross-module events
└── performance/             # Performance-related domain objects
```

**Rules:**
- No dependencies on other layers
- No framework dependencies
- Pure Java/business logic only
- Interfaces for external dependencies

**Example Files:**
- `IngestionBatch.java` - Core business entity
- `BatchId.java` - Value object
- `IIngestionBatchRepository.java` - Repository interface
- `FileStorageService.java` - Domain service interface

### 2. Application Layer (`application/`)

**Purpose:** Orchestrates business workflows, implements use cases.

**Structure:**
```
application/src/main/java/.../application/
├── {entity}/                 # Organized by business entity
│   ├── commands/            # Command handlers (write operations)
│   │   ├── {Action}Command.java
│   │   └── {Action}CommandHandler.java
│   ├── queries/             # Query handlers (read operations)
│   │   ├── {Query}Query.java
│   │   ├── {Query}QueryHandler.java
│   │   └── {Query}Dto.java
│   └── events/              # Event handlers
└── services/                # Application services
```

**Rules:**
- Depends on domain layer only
- Implements use cases and workflows
- Contains DTOs for data transfer
- Handles cross-cutting concerns via interfaces

**Example Files:**
- `UploadFileCommand.java` / `UploadFileCommandHandler.java`
- `BatchStatusQuery.java` / `BatchStatusQueryHandler.java`
- `BatchStatusDto.java`

### 3. Infrastructure Layer (`infrastructure/`)

**Purpose:** Implements external concerns and technical infrastructure.

**Structure:**
```
infrastructure/src/main/java/.../infrastructure/
├── {entity}/                # Entity-specific infrastructure
│   ├── persistence/         # Database implementations
│   │   ├── {Entity}Entity.java
│   │   ├── {Entity}JpaRepository.java
│   │   └── {Entity}RepositoryImpl.java
│   └── {Entity}RepositoryImpl.java
├── auth/                    # Authentication & authorization
├── security/               # Security implementations
├── validation/             # Validation implementations
├── monitoring/             # Logging, metrics, tracing
├── events/                 # Event processing
├── health/                 # Health checks
├── performance/            # Performance optimization
└── configuration/          # Spring configuration
```

**Rules:**
- Implements domain interfaces
- Contains framework-specific code
- Handles external system integration
- Organized by technical concern

**Example Files:**
- `IngestionBatchEntity.java` - JPA entity
- `IngestionBatchRepositoryImpl.java` - Repository implementation
- `IngestionSecurityService.java` - Security implementation
- `IngestionLoggingService.java` - Logging implementation

### 4. Presentation Layer (`presentation/`)

**Purpose:** Handles HTTP requests, API endpoints, and web concerns.

**Structure:**
```
presentation/src/main/java/.../presentation/
├── {entity}/               # Organized by business entity
│   ├── {operation}/        # Organized by operation
│   │   ├── {Operation}Controller.java
│   │   ├── {Operation}Request.java
│   │   └── {Operation}Response.java
├── common/                 # Shared presentation concerns
├── constants/              # API constants
├── exception/              # Exception handlers
├── config/                 # Web configuration
└── health/                 # Health endpoints
```

**Rules:**
- Uses functional RouterFunction pattern (not @RestController)
- Implements IEndpoint interface
- Extends BaseController for common functionality
- Organized by entity and operation
- Contains request/response DTOs

**Example Files:**
- `UploadFileController.java` - Functional endpoint
- `UploadFileResponse.java` - Response DTO
- `IngestionExceptionHandler.java` - Exception handling

## Presentation Layer Standards

### Functional Endpoint Pattern

All controllers must follow this pattern:

```java
@Component
public class {Operation}Controller extends BaseController implements IEndpoint {
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(POST("/api/v1/{module}/{entity}/{operation}"), this::handle)
            .withAttribute("tags", new String[]{"Tag1", "Tag2"})
            .withAttribute("permissions", new String[]{"permission:action"});
    }
    
    private ServerResponse handle(ServerRequest request) {
        // Implementation
    }
}
```

### API Versioning

- All APIs use `/api/v1/` prefix
- Module-specific paths: `/api/v1/{module}/`
- RESTful resource naming

### Error Handling

- Use `IngestionExceptionHandler` for module-specific errors
- Leverage `BaseController` for common error handling
- Return structured `ApiResponse` format

## Dependency Rules

### Allowed Dependencies

```
Presentation → Application → Domain
Presentation → Infrastructure
Infrastructure → Domain
Application → Domain
```

### Forbidden Dependencies

```
Domain → Application ❌
Domain → Infrastructure ❌
Domain → Presentation ❌
Application → Infrastructure ❌
Application → Presentation ❌
Infrastructure → Application ❌
Infrastructure → Presentation ❌
```

## Maven Module Structure

### Parent POM Structure

```xml
<modules>
    <module>domain</module>
    <module>application</module>
    <module>infrastructure</module>
    <module>presentation</module>
</modules>
```

### Layer Dependencies

**Domain:** No external dependencies (pure Java)

**Application:**
```xml
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>{module-name}-domain</artifactId>
</dependency>
```

**Infrastructure:**
```xml
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>{module-name}-domain</artifactId>
</dependency>
<!-- Framework dependencies (Spring, JPA, etc.) -->
```

**Presentation:**
```xml
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>{module-name}-domain</artifactId>
</dependency>
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>{module-name}-application</artifactId>
</dependency>
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>{module-name}-infrastructure</artifactId>
</dependency>
```

## Cross-Module Communication

### 1. Integration Events
- Use domain events for cross-module communication
- Implement outbox pattern for reliability
- Events in `domain/integrationevents/`

### 2. Shared Kernel
- Common utilities in `regtech-core`
- Shared value objects and interfaces
- Cross-cutting concerns (security, logging)

### 3. API Calls
- Direct HTTP calls between modules (when needed)
- Use service discovery patterns
- Implement circuit breakers for resilience

## Implementation Guidelines

### 1. Entity Organization
- Group by business entity (batch, compliance, health)
- Each entity has its own package structure
- Related operations stay together

### 2. Operation Organization
- Commands for write operations
- Queries for read operations
- Events for notifications

### 3. Concern Separation
- Authentication vs Authorization
- Validation vs Business Rules
- Logging vs Monitoring vs Health

### 4. Naming Conventions

**Controllers:** `{Operation}Controller.java`
**Commands:** `{Action}Command.java` / `{Action}CommandHandler.java`
**Queries:** `{Query}Query.java` / `{Query}QueryHandler.java`
**DTOs:** `{Purpose}Dto.java` / `{Purpose}Request.java` / `{Purpose}Response.java`
**Entities:** `{Entity}.java` (domain) / `{Entity}Entity.java` (JPA)
**Repositories:** `I{Entity}Repository.java` (interface) / `{Entity}RepositoryImpl.java` (implementation)

## Migration Strategy

### From Monolithic to Modular

1. **Identify Module Boundaries** - Group by business capability
2. **Extract Domain Layer** - Move entities and business logic
3. **Create Application Layer** - Extract use cases and workflows
4. **Organize Infrastructure** - Group by technical concern
5. **Restructure Presentation** - Organize by entity and operation
6. **Update Dependencies** - Follow dependency rules
7. **Remove Old Structure** - Clean up legacy organization

### Module Creation Checklist

- [ ] Create 4-layer structure (domain, application, infrastructure, presentation)
- [ ] Set up Maven modules with proper dependencies
- [ ] Implement domain entities and value objects
- [ ] Create repository interfaces in domain
- [ ] Implement application services and handlers
- [ ] Set up infrastructure implementations
- [ ] Create functional endpoints in presentation
- [ ] Configure web routing and exception handling
- [ ] Add health checks and monitoring
- [ ] Update documentation

## Benefits

### 1. **Maintainability**
- Clear separation of concerns
- Predictable code organization
- Easy to locate and modify code

### 2. **Testability**
- Isolated layers for unit testing
- Clear interfaces for mocking
- Domain logic testable without frameworks

### 3. **Scalability**
- Modules can be extracted to microservices
- Clear boundaries for team ownership
- Independent deployment capabilities (future)

### 4. **Developer Experience**
- Consistent structure across modules
- Clear patterns to follow
- Reduced cognitive load

## Enforcement

### 1. **Code Reviews**
- Verify layer dependencies
- Check package organization
- Ensure naming conventions

### 2. **Architecture Tests**
- Automated dependency validation
- Package structure verification
- Naming convention checks

### 3. **Documentation**
- Keep this guide updated
- Document module-specific decisions
- Maintain migration guides

---

**This architecture guide is the standard for all RegTech modules. Any deviations must be documented and approved through the architecture review process.**