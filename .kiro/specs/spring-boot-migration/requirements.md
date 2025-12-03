# Requirements Document

## Introduction

This document outlines the requirements for migrating the RegTech Platform from Spring Boot 3.5.6 and Spring Framework 6.x to Spring Boot 4.x and Spring Framework 7.x. The migration will modernize the application stack, adopt Jakarta EE 11, improve observability, and leverage new framework features while maintaining backward compatibility for business functionality.

## Glossary

- **RegTech Platform**: The regulatory technology solution for BCBS 239 compliance
- **Spring Boot**: The application framework currently at version 3.5.6
- **Spring Framework**: The core framework currently at version 6.x
- **Jakarta EE**: The enterprise Java specification, upgrading from version 10 to 11
- **GraalVM**: The native image compilation platform, upgrading to version 25
- **JPA**: Java Persistence API, upgrading from 3.1 to 3.2
- **Servlet**: The web application API, upgrading from 6.0 to 6.1
- **JUnit Jupiter**: The testing framework, version 6 (JUnit 4 support removed)
- **Jackson**: The JSON processing library, upgrading from 2.x to 3.x
- **JSpecify**: The null safety annotation standard replacing JSR 305
- **AOT**: Ahead-of-Time compilation for native images
- **Module**: A functional domain within the RegTech Platform (IAM, Billing, Ingestion, Data Quality, Risk Calculation, Report Generation)

## Requirements

### Requirement 1

**User Story:** As a platform developer, I want to upgrade the Spring Boot and Spring Framework versions, so that the application benefits from the latest features, security updates, and performance improvements.

#### Acceptance Criteria

1. WHEN the system starts THEN the system SHALL use Spring Boot 4.x and Spring Framework 7.x
2. WHEN the system compiles THEN the system SHALL target Jakarta EE 11 with Servlet 6.1, JPA 3.2, and Bean Validation 3.1
3. WHEN the system runs THEN the system SHALL maintain Java 17 as the minimum baseline while supporting Java 21 and Java 25
4. WHEN the system uses Kotlin code THEN the system SHALL support Kotlin 2.2 or higher
5. WHEN the system builds native images THEN the system SHALL use GraalVM 25 with the exact reachability metadata format

### Requirement 2

**User Story:** As a platform developer, I want to migrate from javax.* packages to jakarta.* packages, so that the application complies with Jakarta EE 11 standards.

#### Acceptance Criteria

1. WHEN the system uses dependency injection annotations THEN the system SHALL use jakarta.inject.Inject instead of javax.inject.Inject
2. WHEN the system uses lifecycle annotations THEN the system SHALL use jakarta.annotation.PostConstruct and jakarta.annotation.PreDestroy instead of javax.annotation equivalents
3. WHEN the system uses resource injection THEN the system SHALL use jakarta.annotation.Resource instead of javax.annotation.Resource
4. WHEN the system uses servlet APIs THEN the system SHALL use jakarta.servlet packages instead of javax.servlet packages
5. WHEN the system uses JPA annotations THEN the system SHALL use jakarta.persistence packages instead of javax.persistence packages

### Requirement 3

**User Story:** As a platform developer, I want to upgrade to Jackson 3.x, so that the application uses the latest JSON processing capabilities.

#### Acceptance Criteria

1. WHEN the system processes JSON THEN the system SHALL use Jackson 3.x from the tools.jackson package
2. WHEN the system uses Jackson annotations THEN the system SHALL continue using com.fasterxml.jackson.annotation package for annotations like @JsonView
3. WHEN the system configures Jackson THEN the system SHALL use JsonMapper.builder() instead of Jackson2ObjectMapperBuilder
4. WHEN the system serializes objects THEN the system SHALL maintain compatibility with existing JSON formats
5. WHEN the system deserializes JSON THEN the system SHALL handle both Jackson 2.x and Jackson 3.x formats during the transition period

### Requirement 4

**User Story:** As a platform developer, I want to migrate from JUnit 4 to JUnit Jupiter 6, so that the test suite uses modern testing capabilities.

#### Acceptance Criteria

1. WHEN the system runs tests THEN the system SHALL use JUnit Jupiter 6 exclusively
2. WHEN the system uses Spring test support THEN the system SHALL use SpringExtension instead of SpringRunner
3. WHEN the system uses test rules THEN the system SHALL migrate SpringClassRule and SpringMethodRule to JUnit Jupiter equivalents
4. WHEN the system uses base test classes THEN the system SHALL migrate AbstractJUnit4SpringContextTests to JUnit Jupiter equivalents
5. WHEN the system runs transactional tests THEN the system SHALL migrate AbstractTransactionalJUnit4SpringContextTests to JUnit Jupiter equivalents

### Requirement 5

**User Story:** As a platform developer, I want to adopt JSpecify null safety annotations, so that the application has better null safety guarantees and improved Kotlin interoperability.

#### Acceptance Criteria

1. WHEN the system declares nullable parameters THEN the system SHALL use org.jspecify.annotations.Nullable instead of JSR 305 annotations
2. WHEN the system declares non-null parameters THEN the system SHALL use org.jspecify.annotations.NonNull instead of JSR 305 annotations
3. WHEN the system uses generic types THEN the system SHALL specify nullness for generic type parameters using JSpecify
4. WHEN the system uses arrays THEN the system SHALL specify nullness for array elements using JSpecify
5. WHEN the system uses varargs THEN the system SHALL specify nullness for vararg elements using JSpecify

### Requirement 6

**User Story:** As a platform developer, I want to update HTTP headers handling, so that the application correctly processes headers with the new HttpHeaders API.

#### Acceptance Criteria

1. WHEN the system accesses HTTP headers THEN the system SHALL use the revised HttpHeaders API that no longer extends MultiValueMap
2. WHEN the system requires MultiValueMap operations THEN the system SHALL use HttpHeaders.asMultiValueMap() as a fallback
3. WHEN the system compares header names THEN the system SHALL handle case-insensitive header name comparisons correctly
4. WHEN the system iterates headers THEN the system SHALL treat headers as a collection of name-value pairs
5. WHEN the system modifies headers THEN the system SHALL use HttpHeaders methods instead of map-like operations

### Requirement 7

**User Story:** As a platform developer, I want to update JPA and Hibernate configurations, so that the application leverages JPA 3.2 and Hibernate ORM 7.1/7.2 features.

#### Acceptance Criteria

1. WHEN the system bootstraps JPA THEN the system SHALL use LocalContainerEntityManagerFactoryBean with JPA 3.2 PersistenceConfiguration
2. WHEN the system injects EntityManager THEN the system SHALL support @Inject and @Autowired with qualifier support
3. WHEN the system uses Hibernate-specific features THEN the system SHALL use classes from org.springframework.orm.jpa.hibernate package
4. WHEN the system manages persistence units THEN the system SHALL use SpringPersistenceUnitInfo and adapt to JPA 3.2/4.0 via asStandardPersistenceUnitInfo()
5. WHEN the system uses Hibernate sessions THEN the system SHALL support StatelessSession for transactional operations

### Requirement 8

**User Story:** As a platform developer, I want to update servlet container support, so that the application runs on Servlet 6.1 compliant containers.

#### Acceptance Criteria

1. WHEN the system deploys to Tomcat THEN the system SHALL support Tomcat 11.0 or higher
2. WHEN the system deploys to Jetty THEN the system SHALL support Jetty 12.1 or higher
3. WHEN the system uses mock HTTP support THEN the system SHALL use updated MockHttpServletRequest and MockHttpServletResponse aligned with Servlet 6.1
4. WHEN the system handles null header names THEN the system SHALL process them according to Servlet 6.1 behavior
5. WHEN the system handles null header values THEN the system SHALL process them according to Servlet 6.1 behavior

### Requirement 9

**User Story:** As a platform developer, I want to remove deprecated Spring features, so that the application uses only supported APIs.

#### Acceptance Criteria

1. WHEN the system uses path matching THEN the system SHALL use PathPattern instead of AntPathMatcher
2. WHEN the system uses async features THEN the system SHALL use CompletableFuture instead of ListenableFuture
3. WHEN the system uses HTTP clients THEN the system SHALL not use OkHttp3 support
4. WHEN the system serves static resources THEN the system SHALL use webjars-locator-lite instead of webjars-locator-core
5. WHEN the system uses Spring MVC THEN the system SHALL not use theme support

### Requirement 10

**User Story:** As a platform developer, I want to update observability configuration, so that the application provides comprehensive monitoring with Micrometer 2 and OpenTelemetry.

#### Acceptance Criteria

1. WHEN the system collects metrics THEN the system SHALL use Micrometer 2
2. WHEN the system generates traces THEN the system SHALL use OpenTelemetry integration
3. WHEN the system correlates logs THEN the system SHALL propagate trace context across all modules
4. WHEN the system reports health status THEN the system SHALL include SSL certificate expiration information in expiringChains entry
5. WHEN the system monitors SSL certificates THEN the system SHALL report expiring certificates as VALID instead of WILL_EXPIRE_SOON

### Requirement 11

**User Story:** As a platform developer, I want to update GraalVM native image configuration, so that the application builds efficient native images with GraalVM 25.

#### Acceptance Criteria

1. WHEN the system generates native images THEN the system SHALL use GraalVM 25 with exact reachability metadata format
2. WHEN the system registers reflection hints THEN the system SHALL use simplified type hints without MemberCategory.DECLARED_FIELDS
3. WHEN the system registers resource hints THEN the system SHALL use glob pattern format instead of java.util.regex.Pattern format
4. WHEN the system performs AOT processing THEN the system SHALL benefit from Spring Data AOT Repositories
5. WHEN the system builds native images THEN the system SHALL have faster build times and reduced startup memory footprint

### Requirement 12

**User Story:** As a platform developer, I want to update CORS configuration, so that the application handles pre-flight requests according to Spring Framework 7 behavior.

#### Acceptance Criteria

1. WHEN the system receives CORS pre-flight requests THEN the system SHALL not reject requests when CORS configuration is empty
2. WHEN the system processes OPTIONS requests THEN the system SHALL handle them according to Spring Framework 7 CORS behavior
3. WHEN the system validates CORS origins THEN the system SHALL maintain existing security policies
4. WHEN the system responds to pre-flight requests THEN the system SHALL include appropriate CORS headers
5. WHEN the system handles cross-origin requests THEN the system SHALL enforce configured CORS policies

### Requirement 13

**User Story:** As a platform developer, I want to update test context management, so that the test suite efficiently manages application contexts.

#### Acceptance Criteria

1. WHEN the system runs integration tests THEN the system SHALL pause and resume test contexts to save memory
2. WHEN the system caches test contexts THEN the system SHALL stop background processes in paused contexts
3. WHEN the system uses @Nested test classes THEN the system SHALL support dependency injection in nested test class hierarchies
4. WHEN the system injects dependencies in tests THEN the system SHALL use test-method scoped ExtensionContext
5. WHEN the system runs custom TestExecutionListener implementations THEN the system SHALL work correctly with the new extension context scope

### Requirement 14

**User Story:** As a platform developer, I want to maintain all existing business functionality, so that the migration does not break any features for end users.

#### Acceptance Criteria

1. WHEN users authenticate THEN the system SHALL provide the same authentication capabilities as before the migration
2. WHEN users process billing THEN the system SHALL maintain all payment and subscription functionality
3. WHEN users ingest data THEN the system SHALL process files with the same capabilities as before the migration
4. WHEN users validate data quality THEN the system SHALL apply the same validation rules as before the migration
5. WHEN users calculate risk THEN the system SHALL produce the same risk calculations as before the migration
6. WHEN users generate reports THEN the system SHALL create the same reports as before the migration

### Requirement 15

**User Story:** As a platform developer, I want to update dependency versions, so that the application uses compatible library versions with Spring Boot 4.

#### Acceptance Criteria

1. WHEN the system uses PostgreSQL THEN the system SHALL use a JDBC driver compatible with Spring Boot 4
2. WHEN the system uses Flyway THEN the system SHALL use Flyway 11.7.2 or higher for database migrations
3. WHEN the system uses AWS SDK THEN the system SHALL use a version compatible with Spring Boot 4
4. WHEN the system uses Lombok THEN the system SHALL use a version compatible with Spring Boot 4
5. WHEN the system uses Testcontainers THEN the system SHALL use a version compatible with Spring Boot 4

### Requirement 16

**User Story:** As a platform developer, I want to verify module compatibility, so that all RegTech modules work correctly after the migration.

#### Acceptance Criteria

1. WHEN the regtech-core module starts THEN the module SHALL initialize without errors
2. WHEN the regtech-iam module starts THEN the module SHALL provide authentication and authorization
3. WHEN the regtech-billing module starts THEN the module SHALL process payments and subscriptions
4. WHEN the regtech-ingestion module starts THEN the module SHALL accept and process data files
5. WHEN the regtech-data-quality module starts THEN the module SHALL validate data according to business rules
6. WHEN the regtech-risk-calculation module starts THEN the module SHALL calculate risk metrics
7. WHEN the regtech-report-generation module starts THEN the module SHALL generate regulatory reports
8. WHEN the regtech-app module starts THEN the module SHALL orchestrate all other modules
