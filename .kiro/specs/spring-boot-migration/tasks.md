# Implementation Plan

- [x] 1. Update parent POM and module dependencies





  - Update root pom.xml with Spring Boot 4.x and Spring Framework 7.x versions
  - Update Java version properties (baseline 17, support 21 and 25)
  - Update Kotlin version to 2.2+
  - Add Jakarta EE 11 dependencies
  - Add Jackson 3.x dependencies (tools.jackson)
  - Remove all JUnit 4 dependencies
  - Add JUnit Jupiter dependencies
  - Add JSpecify dependency (org.jspecify:jspecify)
  - Update Flyway to 11.7.2+
  - Update PostgreSQL JDBC driver to Spring Boot 4 compatible version
  - Update AWS SDK to Spring Boot 4 compatible version
  - Update Lombok to Spring Boot 4 compatible version
  - Update Testcontainers to Spring Boot 4 compatible version
  - Update Micrometer to version 2.x
  - Add OpenTelemetry dependencies
  - Update GraalVM version to 25
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 15.1, 15.2, 15.3, 15.4, 15.5_

- [-] 2. Migrate Jakarta EE packages across all modules



  - Find and replace javax.inject.Inject → jakarta.inject.Inject
  - Find and replace javax.inject.Named → jakarta.inject.Named
  - Find and replace javax.annotation.PostConstruct → jakarta.annotation.PostConstruct
  - Find and replace javax.annotation.PreDestroy → jakarta.annotation.PreDestroy
  - Find and replace javax.annotation.Resource → jakarta.annotation.Resource
  - Find and replace javax.servlet.* → jakarta.servlet.*
  - Find and replace javax.persistence.* → jakarta.persistence.*
  - Find and replace javax.validation.* → jakarta.validation.*
  - Verify no javax.* imports remain for Jakarta EE APIs
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_


- [ ] 3. Migrate Jackson to version 3.x
  - Update Jackson core class imports from com.fasterxml.jackson.* → tools.jackson.*
  - Keep annotation imports as com.fasterxml.jackson.annotation.* (unchanged)
  - Replace Jackson2ObjectMapperBuilder with JsonMapper.builder()
  - Update all ObjectMapper instantiation code
  - Review and update custom serializers/deserializers
  - Update Jackson configuration beans
  - Test JSON serialization/deserialization maintains format compatibility
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 4. Migrate all tests to JUnit Jupiter
- [ ] 4.1 Update test annotations and lifecycle methods
  - Replace @RunWith(SpringRunner.class) with @ExtendWith(SpringExtension.class)
  - Replace @Before with @BeforeEach
  - Replace @After with @AfterEach
  - Replace @BeforeClass with @BeforeAll
  - Replace @AfterClass with @AfterAll
  - Replace @Ignore with @Disabled
  - Replace @Test(expected=...) with assertThrows()
  - _Requirements: 4.1, 4.2_

- [ ] 4.2 Update test assertions
  - Replace org.junit.Assert.* with org.junit.jupiter.api.Assertions.*
  - Update assertion method calls for JUnit Jupiter API
  - Consider migrating to AssertJ for fluent assertions
  - _Requirements: 4.1_

- [ ] 4.3 Migrate Spring test support classes
  - Replace SpringClassRule and SpringMethodRule with JUnit Jupiter equivalents
  - Migrate AbstractJUnit4SpringContextTests to JUnit Jupiter base classes
  - Migrate AbstractTransactionalJUnit4SpringContextTests to JUnit Jupiter equivalents
  - Update test context configurations
  - _Requirements: 4.3, 4.4, 4.5_

- [ ] 4.4 Update mock servlet objects
  - Update MockHttpServletRequest usage for Servlet 6.1
  - Update MockHttpServletResponse usage for Servlet 6.1
  - Handle null header name/value behavior changes
  - _Requirements: 8.3, 8.4, 8.5_


- [ ] 5. Migrate to JSpecify null safety annotations
  - Replace JSR 305 @Nullable with org.jspecify.annotations.Nullable
  - Replace JSR 305 @Nonnull with org.jspecify.annotations.NonNull
  - Add nullness specifications for generic type parameters
  - Add nullness specifications for array elements
  - Add nullness specifications for varargs
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 6. Update HTTP headers handling
  - Identify all code using HttpHeaders as MultiValueMap
  - Update to use HttpHeaders methods directly
  - Use HttpHeaders.asMultiValueMap() where MultiValueMap is required
  - Ensure case-insensitive header name comparisons
  - Update header iteration code
  - Update header modification code
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7. Update JPA and Hibernate configurations
  - Update LocalContainerEntityManagerFactoryBean for JPA 3.2
  - Configure PersistenceConfiguration support
  - Update EntityManager injection to support @Inject with qualifiers
  - Update Hibernate-specific code to use org.springframework.orm.jpa.hibernate package
  - Update SpringPersistenceUnitInfo usage with asStandardPersistenceUnitInfo()
  - Configure StatelessSession support if needed
  - Review and update Hibernate properties
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 8. Update servlet container configurations
  - Update embedded Tomcat configuration for version 11.0+
  - Update embedded Jetty configuration for version 12.1+ (if used)
  - Review servlet filter configurations
  - Review servlet listener configurations
  - Test null header handling behavior
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_


- [ ] 9. Remove deprecated Spring Framework APIs
  - Replace AntPathMatcher with PathPattern in all path matching code
  - Update security configurations using path patterns
  - Replace ListenableFuture with CompletableFuture in async methods
  - Update async service method signatures
  - Remove OkHttp3 support if present
  - Migrate to standard HTTP client or RestClient
  - Replace webjars-locator-core with webjars-locator-lite
  - Update WebJars configuration
  - Remove Spring MVC theme support if present
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 10. Update observability configuration
  - Configure Micrometer 2 metrics collection
  - Set up OpenTelemetry integration
  - Configure trace context propagation across modules
  - Update health check endpoints for SSL certificate monitoring
  - Configure SSL certificate expiration reporting (expiringChains entry)
  - Update certificate status reporting (VALID instead of WILL_EXPIRE_SOON)
  - Test metrics collection endpoints
  - Test trace propagation
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 11. Update GraalVM native image configuration
  - Update to GraalVM 25 with exact reachability metadata format
  - Simplify reflection hints (remove MemberCategory.DECLARED_FIELDS)
  - Update resource hints to use glob pattern format
  - Configure Spring Data AOT Repositories
  - Test native image compilation
  - Verify startup time and memory footprint improvements
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 12. Update CORS configuration
  - Review CORS pre-flight request handling
  - Update for Spring Framework 7 empty configuration behavior
  - Ensure OPTIONS requests handled correctly
  - Verify CORS origin validation
  - Test pre-flight response headers
  - Verify cross-origin request enforcement
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_


- [ ] 13. Update test context management
  - Configure test context pausing and resuming
  - Set up background process stopping in paused contexts
  - Enable @Nested test class dependency injection
  - Configure test-method scoped ExtensionContext
  - Update custom TestExecutionListener implementations
  - Test context caching behavior
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ] 14. Verify regtech-core module
  - Start regtech-core module and verify initialization
  - Test shared infrastructure components
  - Verify event processing functionality
  - Test outbox pattern implementation
  - Verify saga management
  - Run regtech-core integration tests
  - _Requirements: 16.1_

- [ ] 15. Verify regtech-iam module
  - Start regtech-iam module and verify initialization
  - Test user authentication flows
  - Test JWT token generation and validation
  - Test refresh token functionality
  - Test authorization and role-based access control
  - Test bank selection functionality
  - Run regtech-iam integration tests
  - _Requirements: 14.1, 16.2_

- [ ] 16. Verify regtech-billing module
  - Start regtech-billing module and verify initialization
  - Test payment processing functionality
  - Test subscription creation and management
  - Test invoice generation
  - Test Stripe integration
  - Test billing account management
  - Run regtech-billing integration tests
  - _Requirements: 14.2, 16.3_


- [ ] 17. Verify regtech-ingestion module
  - Start regtech-ingestion module and verify initialization
  - Test file upload functionality
  - Test batch processing
  - Test data validation during ingestion
  - Test file storage (local and S3)
  - Test batch status tracking
  - Run regtech-ingestion integration tests
  - _Requirements: 14.3, 16.4_

- [ ] 18. Verify regtech-data-quality module
  - Start regtech-data-quality module and verify initialization
  - Test business rules engine
  - Test data quality validation
  - Test quality report generation
  - Test rule exemption handling
  - Test quality metrics collection
  - Run regtech-data-quality integration tests
  - _Requirements: 14.4, 16.5_

- [ ] 19. Verify regtech-risk-calculation module
  - Start regtech-risk-calculation module and verify initialization
  - Test risk calculation algorithms
  - Test exposure classification
  - Test portfolio analysis
  - Test concentration indices calculation
  - Test currency conversion
  - Test calculation results storage
  - Run regtech-risk-calculation integration tests
  - _Requirements: 14.5, 16.6_

- [ ] 20. Verify regtech-report-generation module
  - Start regtech-report-generation module and verify initialization
  - Test HTML report generation
  - Test XBRL report generation
  - Test report metadata management
  - Test report storage (local and S3)
  - Test comprehensive report orchestration
  - Run regtech-report-generation integration tests
  - _Requirements: 14.6, 16.7_


- [ ] 21. Verify regtech-app orchestrator module
  - Start regtech-app with all modules
  - Verify all module contexts load successfully
  - Test inter-module communication
  - Test event propagation across modules
  - Verify database migrations run correctly with Flyway 11.7.2+
  - Test application startup time
  - Run full application integration tests
  - _Requirements: 16.8_

- [ ] 22. Write property test for framework version consistency
- [ ]* 22.1 Implement property test for Property 1
  - **Property 1: Framework Version Consistency**
  - **Validates: Requirements 1.1**
  - Generate test that verifies all module POMs use Spring Boot 4.x and Spring Framework 7.x
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 1: Framework Version Consistency**

- [ ] 23. Write property test for Jakarta EE package consistency
- [ ]* 23.1 Implement property test for Property 2
  - **Property 2: Jakarta EE Package Consistency**
  - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
  - Generate test that scans all Java files for javax.* imports (inject, annotation, servlet, persistence, validation)
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 2: Jakarta EE Package Consistency**

- [ ] 24. Write property test for Jackson package consistency
- [ ]* 24.1 Implement property test for Property 3
  - **Property 3: Jackson Package Consistency**
  - **Validates: Requirements 3.1, 3.2**
  - Generate test that verifies Jackson core uses tools.jackson.* and annotations use com.fasterxml.jackson.annotation.*
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 3: Jackson Package Consistency**

- [ ] 25. Write property test for JUnit Jupiter exclusivity
- [ ]* 25.1 Implement property test for Property 4
  - **Property 4: JUnit Jupiter Exclusivity**
  - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
  - Generate test that scans all test files for JUnit 4 imports and annotations
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 4: JUnit Jupiter Exclusivity**


- [ ] 26. Write property test for JSpecify annotation usage
- [ ]* 26.1 Implement property test for Property 5
  - **Property 5: JSpecify Annotation Usage**
  - **Validates: Requirements 5.1, 5.2**
  - Generate test that verifies all null safety annotations use org.jspecify.annotations.*
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 5: JSpecify Annotation Usage**

- [ ] 27. Write property test for business functionality preservation
- [ ]* 27.1 Implement property test for Property 6
  - **Property 6: Business Functionality Preservation**
  - **Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 14.6**
  - Generate test that compares outputs of business operations before and after migration
  - Test authentication, billing, ingestion, quality validation, risk calculation, report generation
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 6: Business Functionality Preservation**

- [ ] 28. Write property test for module initialization success
- [ ]* 28.1 Implement property test for Property 7
  - **Property 7: Module Initialization Success**
  - **Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8**
  - Generate test that starts each module's Spring context and verifies successful initialization
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 7: Module Initialization Success**

- [ ] 29. Write property test for dependency version compatibility
- [ ]* 29.1 Implement property test for Property 8
  - **Property 8: Dependency Version Compatibility**
  - **Validates: Requirements 15.1, 15.2, 15.3, 15.4, 15.5**
  - Generate test that verifies all third-party dependencies are Spring Boot 4 compatible
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 8: Dependency Version Compatibility**

- [ ] 30. Write property test for test suite completeness
- [ ]* 30.1 Implement property test for Property 9
  - **Property 9: Test Suite Completeness**
  - **Validates: Requirements 4.1, 13.1, 13.2, 13.3, 13.4, 13.5**
  - Generate test that compares test count and coverage before and after migration
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 9: Test Suite Completeness**


- [ ] 31. Write property test for configuration consistency
- [ ]* 31.1 Implement property test for Property 10
  - **Property 10: Configuration Consistency**
  - **Validates: Requirements 10.1, 10.2, 10.3, 12.1, 12.2**
  - Generate test that verifies configuration behavior remains consistent between versions
  - Use jqwik with minimum 100 iterations
  - Tag: **Feature: spring-boot-migration, Property 10: Configuration Consistency**

- [ ] 32. Run full integration test suite
  - Execute all module integration tests
  - Execute cross-module integration tests
  - Verify all tests pass
  - Document any test failures
  - Compare test results to pre-migration baseline
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_

- [ ] 33. Perform manual testing of critical flows
  - Test user authentication and authorization flows
  - Test payment processing end-to-end
  - Test data ingestion and validation pipeline
  - Test risk calculation workflow
  - Test report generation workflow
  - Verify UI functionality (if applicable)
  - Document any issues found
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_

- [ ] 34. Update documentation
  - Document all configuration changes
  - Document API changes affecting application code
  - Document breaking changes in behavior
  - Create migration guide for future reference
  - Document rollback procedure
  - Update README files with new framework versions
  - Document any known issues or limitations
  - _Requirements: All_

- [ ] 35. Final checkpoint - Verify migration success
  - Ensure all tests pass, ask the user if questions arise
  - Verify all 10 correctness properties hold
  - Verify no performance degradation (within 5% of baseline)
  - Verify observability working correctly
  - Verify all modules start successfully
  - Verify all business functionality preserved
  - Document migration completion
  - _Requirements: All_
