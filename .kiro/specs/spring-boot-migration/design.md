# Design Document

## Overview

This design document outlines the technical approach for migrating the RegTech Platform from Spring Boot 3.5.6/Spring Framework 6.x to Spring Boot 4.x/Spring Framework 7.x. The migration will modernize the application stack while maintaining all existing business functionality across seven modules: regtech-core, regtech-iam, regtech-billing, regtech-ingestion, regtech-data-quality, regtech-risk-calculation, and regtech-report-generation.

The migration strategy follows a phased approach with comprehensive testing at each stage to ensure zero business functionality regression. This is a framework upgrade, not a feature development effort - the goal is to update the technical foundation while preserving all existing capabilities.

## Architecture

### High-Level Architecture

The RegTech Platform follows a modular monolith architecture with clear domain boundaries:

```
regtech-app (orchestrator)
├── regtech-core (shared infrastructure)
├── regtech-iam (authentication & authorization)
├── regtech-billing (payments & subscriptions)
├── regtech-ingestion (data file processing)
├── regtech-data-quality (validation rules engine)
├── regtech-risk-calculation (risk metrics computation)
└── regtech-report-generation (regulatory reporting)
```

Each module follows a layered architecture:
- **Domain Layer**: Business logic, entities, value objects
- **Application Layer**: Use cases, command handlers, application services
- **Infrastructure Layer**: Database, external services, file storage
- **Presentation Layer**: REST controllers, DTOs, API routes

### Migration Strategy


The migration will follow a **big-bang approach** with comprehensive pre-migration testing:

1. **Preparation Phase**: Update all dependencies to Spring Boot 4 compatible versions
2. **Code Migration Phase**: Apply all package renames and API updates across all modules
3. **Configuration Phase**: Update application configurations for new framework behavior
4. **Testing Phase**: Execute comprehensive test suite to verify functionality
5. **Validation Phase**: Manual testing of critical business flows

**Rationale**: Given the tight coupling between Spring Boot and Spring Framework versions, and the extensive breaking changes (Jakarta EE 11, Jackson 3.x, JUnit Jupiter migration), an incremental module-by-module approach would create significant integration issues. A coordinated migration ensures all modules move together, reducing the risk of version conflicts and incompatibilities.

### Rollback Strategy

- Maintain a stable branch before migration begins
- Use feature flags for any new framework-specific behavior
- Document all configuration changes for quick reversion
- Keep dependency versions in version control for easy rollback

## Components and Interfaces

### Dependency Management

**Parent POM Updates** (pom.xml):
- Spring Boot version: 3.5.6 → 4.x
- Spring Framework version: 6.x → 7.x
- Java baseline: 17 (with 21 and 25 support)
- Kotlin version: 2.2+
- GraalVM version: 25

**Key Dependency Updates**:

Spring Boot 4.0.0 automatically manages compatible versions of most dependencies through its BOM (Bill of Materials). We should rely on these managed versions and only override when absolutely necessary:

**Managed by Spring Boot 4 (no explicit version needed)**:
- Jackson: 2.x → 3.x (tools.jackson package) - Spring Boot provides compatible version
- Flyway: → 11.7.2+ - Spring Boot provides compatible version
- PostgreSQL JDBC: Spring Boot provides compatible version
- AWS SDK: Spring Boot provides compatible version
- Lombok: Spring Boot provides compatible version
- Micrometer: → 2.x - Spring Boot provides compatible version
- JUnit Jupiter: Spring Boot provides compatible version

**May Need Version Override**:
- Testcontainers: Only override if Spring Boot 4's version doesn't meet our needs

**Rationale**: Spring Boot 4's dependency management ensures all dependencies are compatible with each other. By relying on managed versions, we reduce the risk of version conflicts and benefit from Spring Boot's tested dependency combinations. We should only override versions when we have a specific requirement that Spring Boot's managed version doesn't satisfy.

### Package Migration (Jakarta EE 11)

All javax.* packages must be replaced with jakarta.* equivalents:

**Dependency Injection**:
- `javax.inject.Inject` → `jakarta.inject.Inject`
- `javax.inject.Named` → `jakarta.inject.Named`

**Lifecycle Annotations**:
- `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct`
- `javax.annotation.PreDestroy` → `jakarta.annotation.PreDestroy`
- `javax.annotation.Resource` → `jakarta.annotation.Resource`

**Servlet API**:
- `javax.servlet.*` → `jakarta.servlet.*`
- Affects: All controllers, filters, servlet configurations

**JPA/Persistence**:
- `javax.persistence.*` → `jakarta.persistence.*`
- Affects: All entity classes, repositories, JPA configurations

**Bean Validation**:
- `javax.validation.*` → `jakarta.validation.*`
- Affects: All DTOs, domain objects with validation

**Rationale**: Jakarta EE 11 is the new standard for enterprise Java. The javax → jakarta rename is mandatory for Spring Framework 7 compatibility.


### Jackson 3.x Migration

**Package Changes**:
- Core classes: `com.fasterxml.jackson.*` → `tools.jackson.*`
- Annotations remain: `com.fasterxml.jackson.annotation.*` (unchanged)

**API Changes**:
- `Jackson2ObjectMapperBuilder` → `JsonMapper.builder()`
- Update all ObjectMapper instantiation code
- Review custom serializers/deserializers for API changes

**Affected Components**:
- All REST controllers returning JSON
- All services processing JSON data
- Configuration classes setting up ObjectMapper beans
- Test utilities using ObjectMapper

**Backward Compatibility**:
- Maintain JSON format compatibility during transition
- Support reading both Jackson 2.x and 3.x formats temporarily
- Document any JSON format changes for API consumers

**Rationale**: Jackson 3.x provides better performance and aligns with Jakarta EE 11. The package rename is necessary to avoid conflicts with Jackson 2.x.

### JUnit Jupiter Migration

**Complete Removal of JUnit 4**:
- Remove all JUnit 4 dependencies from POMs
- No backward compatibility - all tests must migrate

**Test Class Updates**:
- `@RunWith(SpringRunner.class)` → `@ExtendWith(SpringExtension.class)`
- `@Before` → `@BeforeEach`
- `@After` → `@AfterEach`
- `@BeforeClass` → `@BeforeAll`
- `@AfterClass` → `@AfterAll`
- `@Ignore` → `@Disabled`

**Assertion Updates**:
- `org.junit.Assert.*` → `org.junit.jupiter.api.Assertions.*`
- Consider using AssertJ for more fluent assertions

**Spring Test Support**:
- `SpringClassRule` and `SpringMethodRule` → JUnit Jupiter equivalents
- `AbstractJUnit4SpringContextTests` → JUnit Jupiter base classes
- `AbstractTransactionalJUnit4SpringContextTests` → JUnit Jupiter equivalents

**Affected Modules**: All modules with test suites

**Rationale**: JUnit 4 support is completely removed in Spring Framework 7. JUnit Jupiter provides better extension model and modern testing features.


### Null Safety with JSpecify

**Migration from JSR 305**:
- `@Nullable` (JSR 305) → `org.jspecify.annotations.Nullable`
- `@Nonnull` (JSR 305) → `org.jspecify.annotations.NonNull`

**Enhanced Null Safety**:
- Specify nullness for generic type parameters
- Specify nullness for array elements
- Specify nullness for varargs

**Benefits**:
- Better Kotlin interoperability
- Improved IDE support for null safety
- More precise null safety contracts

**Affected Components**:
- All public APIs with nullable parameters
- Generic types in domain models
- Service interfaces with optional parameters

**Rationale**: JSpecify is the modern standard for null safety annotations, replacing the deprecated JSR 305. It provides better tooling support and clearer semantics.

### HTTP Headers API Changes

**Breaking Change**: `HttpHeaders` no longer extends `MultiValueMap`

**Migration Strategy**:
- Direct map operations → Use `HttpHeaders` methods
- Where `MultiValueMap` is required → Use `HttpHeaders.asMultiValueMap()`
- Header name comparisons → Ensure case-insensitive handling

**Affected Components**:
- REST controllers manipulating headers
- HTTP client code
- Interceptors and filters working with headers
- Test code using `MockHttpServletRequest`/`MockHttpServletResponse`

**Rationale**: This change improves type safety and makes the API more explicit about header semantics.


### JPA and Hibernate Updates

**JPA 3.2 Features**:
- New `PersistenceConfiguration` API
- Enhanced `EntityManager` injection with qualifiers
- Support for `@Inject` alongside `@Autowired`

**Hibernate ORM 7.1/7.2**:
- Use `org.springframework.orm.jpa.hibernate` package for Hibernate-specific features
- `SpringPersistenceUnitInfo` with `asStandardPersistenceUnitInfo()` adapter
- `StatelessSession` support for transactional operations

**Configuration Updates**:
- Update `LocalContainerEntityManagerFactoryBean` configuration
- Review Hibernate properties for deprecated settings
- Update entity scanning configuration

**Affected Components**:
- All JPA repositories
- Entity manager factory configurations
- Transaction management setup
- Database initialization code

**Rationale**: JPA 3.2 and Hibernate 7.x provide better performance and align with Jakarta EE 11 standards.

### Servlet Container Support

**Minimum Versions**:
- Tomcat: 11.0+
- Jetty: 12.1+

**Servlet 6.1 Compliance**:
- Update servlet configurations
- Review null header handling behavior
- Update mock servlet objects in tests

**Affected Components**:
- Embedded servlet container configuration
- Servlet filters and listeners
- Test configurations using mock servlets

**Rationale**: Servlet 6.1 is required for Jakarta EE 11 compliance and provides improved performance.


### Deprecated API Removal

**Path Matching**:
- `AntPathMatcher` → `PathPattern`
- Update all URL pattern matching code
- Review security configurations using path patterns

**Async Support**:
- `ListenableFuture` → `CompletableFuture`
- Update all async service methods
- Review async configuration

**HTTP Client**:
- Remove OkHttp3 support (if used)
- Migrate to standard HTTP client or RestClient

**Static Resources**:
- `webjars-locator-core` → `webjars-locator-lite`
- Update WebJars configuration

**Spring MVC**:
- Remove theme support (if used)
- Update any theme-related configurations

**Affected Components**:
- Security configurations with path matchers
- Async service methods
- Static resource handling
- WebJars configuration

**Rationale**: These APIs are removed in Spring Framework 7 to reduce maintenance burden and encourage modern alternatives.

## Data Models

### No Domain Model Changes

The migration does not require changes to domain models beyond:
- Package imports (javax → jakarta)
- Null safety annotations (JSR 305 → JSpecify)
- JPA annotations package updates

All business entities, value objects, and aggregates remain unchanged in structure and behavior.


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Framework Version Consistency
*For any* module in the RegTech Platform, the Spring Boot version should be 4.x and Spring Framework version should be 7.x after migration
**Validates: Requirements 1.1**

### Property 2: Jakarta EE Package Consistency
*For any* Java source file in the codebase, there should be no remaining javax.* imports for Jakarta EE APIs (inject, annotation, servlet, persistence, validation)
**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

### Property 3: Jackson Package Consistency
*For any* Java source file using Jackson core classes, the imports should use tools.jackson.* packages, while annotation imports remain com.fasterxml.jackson.annotation.*
**Validates: Requirements 3.1, 3.2**

### Property 4: JUnit Jupiter Exclusivity
*For any* test class in the codebase, there should be no JUnit 4 imports or annotations (org.junit.Test, @RunWith, @Before, etc.)
**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

### Property 5: JSpecify Annotation Usage
*For any* null safety annotation in the codebase, it should use org.jspecify.annotations.* instead of JSR 305 annotations
**Validates: Requirements 5.1, 5.2**

### Property 6: Business Functionality Preservation
*For any* business operation (authentication, billing, ingestion, quality validation, risk calculation, report generation), the output should be identical to pre-migration behavior given the same inputs
**Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 14.6**

### Property 7: Module Initialization Success
*For any* RegTech module, the Spring application context should start successfully without errors
**Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8**


### Property 8: Dependency Version Compatibility
*For any* third-party dependency (PostgreSQL, Flyway, AWS SDK, Lombok, Testcontainers), the version should be compatible with Spring Boot 4.x
**Validates: Requirements 15.1, 15.2, 15.3, 15.4, 15.5**

### Property 9: Test Suite Completeness
*For any* existing test in the pre-migration codebase, there should be an equivalent migrated test that passes in the post-migration codebase
**Validates: Requirements 4.1, 13.1, 13.2, 13.3, 13.4, 13.5**

### Property 10: Configuration Consistency
*For any* application configuration property, the behavior should remain consistent between Spring Boot 3.5.6 and 4.x unless explicitly documented as changed
**Validates: Requirements 10.1, 10.2, 10.3, 12.1, 12.2**

## Error Handling

### Migration-Specific Error Scenarios

**Compilation Errors**:
- Missing Jakarta EE dependencies → Add correct Jakarta EE 11 dependencies
- Jackson API incompatibilities → Update to Jackson 3.x API patterns
- JUnit 4 references → Complete migration to JUnit Jupiter

**Runtime Errors**:
- ClassNotFoundException for javax.* classes → Verify all package renames complete
- NoSuchMethodError for changed APIs → Update to new API signatures
- Bean creation failures → Review configuration for Spring Framework 7 compatibility

**Test Failures**:
- JUnit 4 runner not found → Migrate to JUnit Jupiter extensions
- Mock servlet API mismatches → Update to Servlet 6.1 mock objects
- Context loading failures → Review test configuration for compatibility

### Error Recovery Strategy

1. **Incremental Compilation**: Fix compilation errors module by module
2. **Dependency Resolution**: Use Maven dependency:tree to identify conflicts
3. **Test Isolation**: Run test suites per module to isolate failures
4. **Configuration Validation**: Use Spring Boot's configuration processor to validate properties


## Testing Strategy

### Dual Testing Approach

The migration requires both unit testing and property-based testing to ensure correctness:

**Unit Tests**:
- Verify specific migration scenarios (e.g., Jakarta package imports work correctly)
- Test individual component behavior after migration
- Validate configuration changes
- Test error handling for migration-specific issues

**Property-Based Tests**:
- Verify universal properties across all modules (e.g., no javax.* imports remain)
- Test business functionality preservation with generated inputs
- Validate framework version consistency across all POMs
- Test that all modules start successfully with various configurations

### Testing Framework

**Property-Based Testing Library**: jqwik (already in use in the codebase)
- Minimum iterations per property test: 100
- Each property test must reference the design document property it implements
- Tag format: `**Feature: spring-boot-migration, Property {number}: {property_text}**`

### Test Categories

**1. Compilation Tests**:
- Verify all modules compile with Spring Boot 4.x dependencies
- Check for no javax.* package references in compiled code
- Validate Jackson 3.x usage

**2. Context Loading Tests**:
- Test each module's Spring context loads successfully
- Verify bean creation and dependency injection
- Test with various Spring profiles

**3. Integration Tests**:
- Run existing integration test suites
- Verify database migrations work with new Flyway version
- Test inter-module communication

**4. Business Functionality Tests**:
- Authentication flows (IAM module)
- Payment processing (Billing module)
- Data ingestion (Ingestion module)
- Quality validation (Data Quality module)
- Risk calculations (Risk Calculation module)
- Report generation (Report Generation module)

**5. Observability Tests**:
- Verify Micrometer 2 metrics collection
- Test OpenTelemetry trace propagation
- Validate health check endpoints

**6. Native Image Tests** (if applicable):
- Test GraalVM 25 native image compilation
- Verify AOT processing
- Test native image startup and runtime


### Test Execution Strategy

**Phase 1: Pre-Migration Baseline**
- Run full test suite on Spring Boot 3.5.6
- Document all passing tests as baseline
- Identify any existing test failures

**Phase 2: Post-Migration Validation**
- Run full test suite on Spring Boot 4.x
- Compare results to baseline
- Investigate any new failures

**Phase 3: Property Verification**
- Execute property-based tests
- Verify all correctness properties hold
- Document any property violations

**Phase 4: Manual Testing**
- Execute critical business flows manually
- Verify UI functionality (if applicable)
- Test edge cases not covered by automated tests

### Test Coverage Requirements

- Maintain or improve existing code coverage
- 100% coverage of migration-specific code changes
- All correctness properties must have corresponding property tests
- All modules must have context loading tests

## Configuration Management

### Application Configuration Updates

**Observability Configuration**:
- Update Micrometer configuration for version 2
- Configure OpenTelemetry integration
- Update health check configurations for SSL certificate monitoring

**CORS Configuration**:
- Review CORS pre-flight request handling
- Update for Spring Framework 7 behavior
- Ensure security policies remain enforced

**Test Context Configuration**:
- Enable context pausing/resuming for memory efficiency
- Configure @Nested test class support
- Update TestExecutionListener implementations

### Module-Specific Configurations

Each module's application-{module}.yml may require updates for:
- New framework properties
- Deprecated property replacements
- Observability settings
- Test configurations


## Migration Execution Plan

### Phase 1: Dependency Updates (Requirements 1, 15)

**Objective**: Update to Spring Boot 4 and leverage its dependency management

**Actions**:
1. Update parent POM with Spring Boot 4.0.0 version
2. Remove explicit version overrides for dependencies managed by Spring Boot 4:
   - Remove Spring Framework version (managed by Spring Boot)
   - Remove Flyway version (managed by Spring Boot)
   - Remove PostgreSQL JDBC version (managed by Spring Boot)
   - Remove AWS SDK version (managed by Spring Boot)
   - Remove Lombok version (managed by Spring Boot)
   - Remove Micrometer version (managed by Spring Boot)
   - Remove JUnit Jupiter version (managed by Spring Boot)
3. Keep only necessary version overrides (e.g., Testcontainers if needed)
4. Remove JUnit 4 dependencies completely
5. Verify Jakarta EE 11 dependencies are provided by Spring Boot
6. Verify Jackson 3.x dependencies are provided by Spring Boot
7. Add JSpecify dependency (not managed by Spring Boot)

**Validation**: Maven build completes dependency resolution without conflicts; verify Spring Boot 4's managed versions are being used

### Phase 2: Package Migration (Requirements 2, 3, 5)

**Objective**: Migrate all package imports to Jakarta EE 11 and Jackson 3.x

**Actions**:
1. Find and replace javax.inject → jakarta.inject
2. Find and replace javax.annotation → jakarta.annotation
3. Find and replace javax.servlet → jakarta.servlet
4. Find and replace javax.persistence → jakarta.persistence
5. Find and replace javax.validation → jakarta.validation
6. Update Jackson core imports to tools.jackson.*
7. Keep Jackson annotation imports as com.fasterxml.jackson.annotation.*
8. Replace JSR 305 annotations with JSpecify annotations

**Validation**: No javax.* imports remain for Jakarta EE APIs; Jackson 3.x imports correct

### Phase 3: API Updates (Requirements 6, 7, 8, 9)

**Objective**: Update code to use new Spring Framework 7 APIs

**Actions**:
1. Update HttpHeaders usage (remove MultiValueMap assumptions)
2. Update JPA configuration for JPA 3.2
3. Update Hibernate configuration for ORM 7.x
4. Update servlet container configurations
5. Replace AntPathMatcher with PathPattern
6. Replace ListenableFuture with CompletableFuture
7. Remove OkHttp3 usage (if any)
8. Update WebJars configuration to use webjars-locator-lite
9. Remove theme support (if any)
10. Update Jackson ObjectMapper instantiation to use JsonMapper.builder()

**Validation**: Code compiles without errors; no deprecated API usage warnings


### Phase 4: Test Migration (Requirements 4, 13)

**Objective**: Migrate all tests to JUnit Jupiter

**Actions**:
1. Replace @RunWith(SpringRunner.class) with @ExtendWith(SpringExtension.class)
2. Update test lifecycle annotations (@Before → @BeforeEach, etc.)
3. Update assertions to JUnit Jupiter API
4. Migrate Spring test base classes
5. Update test context configurations
6. Add support for @Nested test classes
7. Update mock servlet objects to Servlet 6.1

**Validation**: All tests compile and run successfully

### Phase 5: Configuration Updates (Requirements 10, 11, 12)

**Objective**: Update application configurations for Spring Framework 7

**Actions**:
1. Update observability configuration for Micrometer 2
2. Configure OpenTelemetry integration
3. Update health check configuration for SSL monitoring
4. Review and update CORS configuration
5. Update test context management configuration
6. Update GraalVM native image configuration (if applicable)
7. Review all application-{module}.yml files for deprecated properties

**Validation**: Application starts successfully with all configurations loaded

### Phase 6: Module Validation (Requirements 14, 16)

**Objective**: Verify all modules work correctly after migration

**Actions**:
1. Start regtech-app and verify all modules initialize
2. Test IAM authentication and authorization flows
3. Test Billing payment and subscription processing
4. Test Ingestion data file processing
5. Test Data Quality validation rules
6. Test Risk Calculation metrics computation
7. Test Report Generation regulatory reports
8. Run full integration test suite
9. Execute property-based tests for all correctness properties

**Validation**: All business functionality works identically to pre-migration behavior


## Risk Assessment and Mitigation

### High-Risk Areas

**1. Jakarta EE Package Migration**
- **Risk**: Missing package renames causing runtime ClassNotFoundException
- **Mitigation**: Automated search and replace; comprehensive compilation testing; runtime verification

**2. Jackson 3.x Migration**
- **Risk**: JSON serialization format changes breaking API contracts
- **Mitigation**: Maintain backward compatibility; test with existing JSON payloads; document any format changes

**3. JUnit Migration**
- **Risk**: Test coverage loss during migration
- **Mitigation**: Migrate tests incrementally; verify test count before/after; maintain test coverage metrics

**4. Business Functionality Preservation**
- **Risk**: Subtle behavior changes in framework affecting business logic
- **Mitigation**: Comprehensive integration testing; property-based testing; manual testing of critical flows

**5. Dependency Conflicts**
- **Risk**: Transitive dependency conflicts causing runtime issues
- **Mitigation**: Use Maven dependency:tree; resolve conflicts explicitly; test with clean Maven repository

### Medium-Risk Areas

**1. Configuration Changes**
- **Risk**: Deprecated properties causing unexpected behavior
- **Mitigation**: Review Spring Boot 4 migration guide; test all configuration profiles

**2. HTTP Headers API Changes**
- **Risk**: Code assuming MultiValueMap behavior breaking
- **Mitigation**: Search for HttpHeaders usage; update to explicit API calls

**3. Observability Integration**
- **Risk**: Metrics and traces not working correctly
- **Mitigation**: Test observability endpoints; verify trace propagation

### Low-Risk Areas

**1. GraalVM Native Image**
- **Risk**: Native image compilation issues
- **Mitigation**: Test native image build; update reachability metadata if needed

**2. Servlet Container Updates**
- **Risk**: Tomcat/Jetty compatibility issues
- **Mitigation**: Test with embedded containers; verify deployment


## Success Criteria

The migration is considered successful when:

1. **Compilation**: All modules compile without errors using Spring Boot 4.x and Spring Framework 7.x
2. **Package Migration**: Zero javax.* imports remain for Jakarta EE APIs
3. **Test Suite**: All existing tests pass after migration to JUnit Jupiter
4. **Context Loading**: All module Spring contexts load successfully
5. **Business Functionality**: All business operations produce identical results to pre-migration
6. **Integration Tests**: Full integration test suite passes
7. **Property Tests**: All 10 correctness properties verified
8. **Performance**: No significant performance degradation (within 5% of baseline)
9. **Observability**: Metrics and traces working correctly with Micrometer 2 and OpenTelemetry
10. **Documentation**: All configuration changes documented

## Documentation Requirements

### Technical Documentation

1. **Migration Guide**: Step-by-step instructions for the migration process
2. **API Changes**: Document all API changes affecting application code
3. **Configuration Changes**: List all configuration property changes
4. **Breaking Changes**: Document any breaking changes in behavior
5. **Rollback Procedure**: Instructions for reverting the migration

### Code Documentation

1. **Migration Comments**: Add comments explaining non-obvious migration changes
2. **Deprecated API Replacements**: Document why specific APIs were replaced
3. **Configuration Examples**: Provide examples of new configuration patterns

### Testing Documentation

1. **Test Migration Guide**: Document test migration patterns
2. **Property Test Descriptions**: Explain each correctness property
3. **Test Coverage Report**: Document test coverage before and after migration

## Timeline Estimate

- **Phase 1 (Dependency Updates)**: 1-2 days
- **Phase 2 (Package Migration)**: 2-3 days
- **Phase 3 (API Updates)**: 3-5 days
- **Phase 4 (Test Migration)**: 3-5 days
- **Phase 5 (Configuration Updates)**: 1-2 days
- **Phase 6 (Module Validation)**: 2-3 days
- **Buffer for Issues**: 3-5 days

**Total Estimated Duration**: 15-25 days

This timeline assumes full-time work on the migration and may vary based on the size of the codebase and complexity of custom integrations.
