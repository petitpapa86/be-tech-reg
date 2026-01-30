# Feature Development Protocol

This document outlines the mandatory step-by-step protocol for adding new features to any module in the RegTech platform. Adhering to this protocol ensures consistency with the **Modular Monolith + Clean Architecture** pattern.

## Prerequisite: Understand the Architecture
Before starting, ensure you have read:
1. `copilot-instructions.md` (General Architecture & Stack)
2. `CLEAN_ARCH_GUIDE.md` (Detailed Layering Rules)
3. `horvat style.md` (Emergent Design Mindset & Coding Philosophy)

---

## Phase 1: Domain Discovery & Modeling (The "What")
**Goal**: Define the business concepts and rules without thinking about databases or APIs.

1.  **Identify Entities & Value Objects**:
    *   Create classes in `regtech-{module}/domain`.
    *   Use **Java Records** for Value Objects (immutable).
    *   Use **Classes** for Entities (identity + state change).
2.  **Implement Business Logic**:
    *   Methods on Entities/Aggregates should handle state transitions.
    *   **Rule**: NO Spring annotations (`@Service`, `@Autowired`) in this layer.
    *   **Rule**: NO Persistence annotations (`@Entity`, `@Table`) in this layer.
3.  **Unit Test**:
    *   Write fast unit tests in `regtech-{module}/domain/src/test`.

## Phase 2: Application Layer (The "Orchestration")
**Goal**: Define how the outside world interacts with your domain.

1.  **Define Ports (Interfaces)**:
    *   Create interfaces in `application/port` for any external dependency (e.g., `UserRepository`, `EmailSender`).
2.  **Implement Use Cases**:
    *   Create classes in `application/usecase` (or functional grouping).
    *   Annotate with `@Component` or `@Service`.
    *   Orchestrate flow: Load Data (via Port) -> Call Domain Logic -> Save Data (via Port).
    *   **Rule**: Do not use HTTP or Controller logic here.

## Phase 3: Infrastructure Layer (The "Realization")
**Goal**: Implement the technical details (Database, External APIs).

1.  **Implement Persistence**:
    *   Create JPA Entities in `infrastructure/entity`.
    *   Implement Repository Ports in `infrastructure/repository`.
    *   Map between Domain Objects and JPA Entities (Mappers).
2.  **Implement Adapters**:
    *   Implement any other ports (e.g., calling another module via `RestClient` or `EventPublisher`).
3.  **Integration Test**:
    *   Write integration tests in `infrastructure/src/test` to verify database queries and external connections.

## Phase 4: Presentation Layer (The "Entry Point")
**Goal**: Expose the feature to the user or other systems.

1.  **API Definition**:
    *   **Rule**: Use **Functional Endpoints** (`RouterFunction<ServerResponse>`), NOT annotated `@RestController`.
    *   Implement `IEndpoint` (or equivalent) and expose a `@Bean` for routes.
    *   Use `RouterAttributes` for permissions, tags, and descriptions.
    *   Define Request/Response DTOs.
2.  **Event Listeners**:
    *   If triggering from an event, create Listeners in `presentation/integration/listener`.
3.  **Validation**:
    *   Validate inputs (e.g., `@Valid`, `jakarta.validation`) at this boundary.

## Phase 5: Verification & Cleanup
1.  **Run All Tests**: Ensure `mvn test` passes for the module.
2.  **Check Linter**: Ensure no unused imports or checkstyle violations.
3.  **Update Documentation**: If this is a major feature, update `README.md` in the module.

---

## Quick Checklist
- [ ] Logic is in Domain (not Service).
- [ ] Domain has NO dependencies on Spring/JPA.
- [ ] Database schema changes are in `flyway` migrations.
- [ ] New Dependencies are added to `pom.xml` (verify versions).
