# Implementation Plan

- [x] 1. Audit and document current configuration





  - Review all existing configuration files across all modules
  - Document which modules use S3, thread pools, and async processing
  - Identify duplicate configuration between root and module files
  - Create configuration inventory document
  - _Requirements: 1.1, 1.2, 8.5_

- [x] 2. Create module-specific configuration files





- [x] 2.1 Create application-ingestion.yml


  - Create regtech-ingestion/infrastructure/src/main/resources/application-ingestion.yml
  - Move ingestion-specific configuration from root application.yml
  - Add S3 storage configuration with profile overrides
  - Add async thread pool configuration
  - Add inline documentation comments
  - _Requirements: 1.2, 1.3, 2.2, 13.1_

- [x] 2.2 Create application-data-quality.yml


  - Create regtech-data-quality/infrastructure/src/main/resources/application-data-quality.yml
  - Move data-quality-specific configuration from root application.yml
  - Add S3 storage configuration with profile overrides
  - Add async thread pool configuration
  - Add rules engine configuration
  - _Requirements: 1.2, 1.3, 2.2, 13.1_

- [x] 2.3 Create application-risk-calculation.yml


  - Create regtech-risk-calculation/infrastructure/src/main/resources/application-risk-calculation.yml
  - Move risk-calculation-specific configuration from root application.yml
  - Add S3 storage configuration with profile overrides
  - Add async thread pool configuration
  - Add currency conversion and geographic classification config
  - _Requirements: 1.2, 1.3, 2.2, 13.1_

- [x] 2.4 Create application-report-generation.yml

  - Create regtech-report-generation/infrastructure/src/main/resources/application-report-generation.yml
  - Move report-generation-specific configuration from root application.yml
  - Add S3 storage configuration with profile overrides
  - Add async thread pool configuration
  - Add Resilience4j circuit breaker configuration
  - _Requirements: 1.2, 1.3, 2.2, 13.1_

- [x] 2.5 Create application-billing.yml


  - Create regtech-billing/infrastructure/src/main/resources/application-billing.yml
  - Move billing-specific configuration from root application.yml
  - Add Stripe configuration
  - Add dunning and invoicing configuration
  - Add scheduling configuration
  - _Requirements: 1.2, 1.3_

- [x] 2.6 Create application-iam.yml


  - Create regtech-iam/infrastructure/src/main/resources/application-iam.yml
  - Move IAM-specific configuration from root application.yml (if any module-specific settings exist)
  - Keep security configuration in root application.yml (shared concern)
  - _Requirements: 1.2, 1.3_

- [x] 3. Reorganize root application.yml





- [x] 3.1 Consolidate shared infrastructure configuration


  - Keep only shared configuration in root application.yml
  - Organize into clear sections: Spring, Database, Logging, Metrics, Security, Events
  - Add section comments explaining what belongs in root vs modules
  - _Requirements: 1.1, 1.4, 8.1_

- [x] 3.2 Consolidate security configuration

  - Move all security configuration under iam.security section
  - Create centralized public-paths list
  - Add comments documenting why each path is public
  - Include health endpoints from all modules
  - _Requirements: 11.1, 11.2, 11.5_

- [x] 3.3 Organize Flyway migration locations

  - Configure Flyway to load migrations from all module directories
  - Document migration location pattern
  - _Requirements: 3.2_

- [x] 3.4 Add profile-specific overrides

  - Create development profile section with local storage and reduced thread pools
  - Create production profile section with S3 storage and optimized settings
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 4. Update configuration properties classes





- [x] 4.1 Update IngestionProperties class


  - Update @ConfigurationProperties prefix to "ingestion"
  - Add nested classes for file, storage, async, performance
  - Add validation annotations (@NotNull, @Min, @Max)
  - _Requirements: 6.2, 6.5, 10.3_

- [x] 4.2 Update DataQualityProperties class


  - Create or update @ConfigurationProperties class for data-quality module
  - Add nested classes for storage, async, rules-engine
  - Add validation annotations
  - _Requirements: 6.2, 6.5, 10.3_


- [x] 4.3 Update RiskCalculationProperties class

  - Update @ConfigurationProperties prefix to "risk-calculation"
  - Add nested classes for storage, async, currency, geographic, concentration
  - Add validation annotations
  - _Requirements: 6.2, 6.5, 10.3_


- [x] 4.4 Update ReportGenerationProperties class

  - Create or update @ConfigurationProperties class for report-generation module
  - Add nested classes for s3, async, coordination, performance, retry
  - Add validation annotations
  - _Requirements: 6.2, 6.5, 10.3_

- [x] 4.5 Update BillingProperties class


  - Update @ConfigurationProperties prefix to "billing"
  - Add nested classes for stripe, tiers, dunning, invoices, scheduling
  - Add validation annotations
  - _Requirements: 6.2, 6.5, 10.3_

- [x] 4.6 Update IAMProperties class


  - Update @ConfigurationProperties prefix to "iam"
  - Add nested classes for security (jwt, password, oauth2, public-paths, authorization)
  - Add validation annotations
  - _Requirements: 6.2, 6.5, 10.3, 11.1_

- [x] 5. Update SecurityFilter to use configuration





- [x] 5.1 Inject public paths from configuration


  - Modify SecurityFilter constructor to accept IAMProperties
  - Replace hardcoded publicPaths Set with configuration-driven list
  - Add logging for loaded public paths
  - _Requirements: 11.2, 11.3_

- [x] 5.2 Add configuration validation


  - Validate public paths are not empty
  - Validate path patterns are valid
  - Log warning if deprecated paths are used
  - _Requirements: 10.1, 10.2_

- [x] 6. Create async configuration classes for modules





- [x] 6.1 Create IngestionAsyncConfiguration


  - Create @Configuration class with @EnableAsync
  - Bind to ingestion.async properties
  - Create ingestionTaskExecutor bean
  - Add validation for pool sizes
  - _Requirements: 13.1, 13.2, 13.5_

- [x] 6.2 Create DataQualityAsyncConfiguration


  - Create @Configuration class with @EnableAsync
  - Bind to data-quality.async properties
  - Create dataQualityTaskExecutor bean
  - Add validation for pool sizes
  - _Requirements: 13.1, 13.2, 13.5_



- [x] 6.3 Create RiskCalculationAsyncConfiguration





  - Create @Configuration class with @EnableAsync
  - Bind to risk-calculation.async properties
  - Create riskCalculationTaskExecutor bean
  - Add validation for pool sizes


  - _Requirements: 13.1, 13.2, 13.5_

- [x] 6.4 Create ReportGenerationAsyncConfiguration




  - Create @Configuration class with @EnableAsync
  - Bind to report-generation.async properties
  - Create reportGenerationTaskExecutor bean
  - Add validation for pool sizes
  - _Requirements: 13.1, 13.2, 13.5_

- [x] 7. Update module routes to use consistent security pattern





- [x] 7.1 Audit all module route definitions


  - Review all RouterFunction definitions across modules
  - Identify routes that should be public
  - Identify routes that require authentication
  - Identify routes that require specific permissions
  - _Requirements: 12.1, 12.2_

- [x] 7.2 Update ingestion routes


  - Ensure all routes use RouterAttributes.withAttributes
  - Add health endpoints to public paths configuration
  - Document permission requirements
  - _Requirements: 12.2, 12.3, 12.4_


- [x] 7.3 Update data-quality routes

  - Ensure all routes use RouterAttributes.withAttributes
  - Add health endpoints to public paths configuration
  - Document permission requirements
  - _Requirements: 12.2, 12.3, 12.4_

- [x] 7.4 Update risk-calculation routes


  - Ensure all routes use RouterAttributes.withAttributes
  - Add health endpoints to public paths configuration
  - Document permission requirements
  - _Requirements: 12.2, 12.3, 12.4_

- [x] 7.5 Update report-generation routes


  - Ensure all routes use RouterAttributes.withAttributes
  - Add health endpoints to public paths configuration
  - Document permission requirements
  - _Requirements: 12.2, 12.3, 12.4_

- [ ]* 8. Create configuration validation tests
- [ ]* 8.1 Create ConfigurationPropertiesTest
  - Test that all @ConfigurationProperties classes bind correctly
  - Test default values when properties not specified
  - Test validation annotations work correctly
  - _Requirements: 6.5, 10.1, 10.2_

- [ ]* 8.2 Create ProfileOverrideTest
  - Test development profile overrides default values
  - Test production profile overrides default values
  - Test profile-specific values take precedence
  - _Requirements: 7.1, 7.2, 7.3_

- [ ]* 8.3 Create EnvironmentVariableTest
  - Test ${VAR_NAME} syntax resolves correctly
  - Test ${VAR_NAME:default} provides fallback values
  - Test missing required environment variables cause startup failure
  - _Requirements: 2.5, 3.4, 7.5_

- [ ]* 8.4 Create NumericRangeValidationTest
  - Test thread-pool-size validation (must be > 0)
  - Test core-pool-size <= max-pool-size validation
  - Test timeout values validation
  - Test file size limits validation
  - _Requirements: 10.3, 13.5_

- [ ]* 9. Create integration tests
- [ ]* 9.1 Create ApplicationContextLoadingTest
  - Test application starts successfully with all modules
  - Test all configuration files are found and loaded
  - Test all @ConfigurationProperties beans are created
  - _Requirements: 1.1, 1.2, 6.1_

- [ ]* 9.2 Create S3ConfigurationIntegrationTest
  - Test S3 client creation with configuration from each module
  - Test that S3 operations use correct bucket/prefix
  - Test fallback to local storage when S3 is unavailable
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 9.3 Create SecurityConfigurationIntegrationTest
  - Test that SecurityFilter loads public paths from configuration
  - Test that public paths are accessible without authentication
  - Test that protected paths require authentication
  - Test that RouterAttributes permissions are enforced correctly
  - _Requirements: 11.2, 11.3, 11.4, 12.2, 12.4_

- [ ]* 9.4 Create ThreadPoolConfigurationIntegrationTest
  - Test that each module creates its own task executor
  - Test thread pool sizes match configuration
  - Test profile-specific thread pool sizes
  - Test async operations use correct executor
  - _Requirements: 13.1, 13.2, 13.3_

- [x] 10. Checkpoint - Ensure all tests pass





  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Remove duplicate configuration




- [x] 11.1 Remove module-specific config from root application.yml


  - Remove ingestion configuration from root
  - Remove data-quality configuration from root
  - Remove risk-calculation configuration from root
  - Remove report-generation configuration from root
  - Remove billing configuration from root
  - Keep only shared infrastructure configuration
  - _Requirements: 1.4, 9.3_

- [x] 11.2 Verify backward compatibility


  - Test that application still starts after removing duplicates
  - Test that all modules load their configuration correctly
  - Test that no configuration is missing
  - _Requirements: 9.1, 9.2_

- [ ]* 12. Create documentation
- [ ]* 12.1 Create CONFIGURATION_REFERENCE.md
  - Document all configuration properties by module
  - Document valid values and defaults
  - Provide examples for common scenarios
  - Document environment variables
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ]* 12.2 Create CONFIGURATION_MIGRATION.md
  - Document the migration from old to new structure
  - Provide mapping of old paths to new paths
  - Document troubleshooting common issues
  - Document rollback procedure
  - _Requirements: 9.1, 9.4, 9.5_

- [ ]* 12.3 Update deployment documentation
  - Document environment-specific configuration instructions
  - Document required environment variables
  - Document profile selection guidance
  - Document Hetzner deployment specifics
  - _Requirements: 7.5, 8.4_

- [ ] 13. Final validation and cleanup
- [ ] 13.1 Run full test suite
  - Run all unit tests
  - Run all integration tests
  - Run all configuration validation tests
  - Verify all tests pass
  - _Requirements: 10.5_

- [ ] 13.2 Verify application startup in all profiles
  - Test startup with development profile
  - Test startup with production profile
  - Test startup with no profile (defaults)
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 13.3 Clean up deprecated configuration
  - Remove any deprecated configuration paths
  - Remove temporary backward compatibility code
  - Update all documentation references
  - _Requirements: 9.5_

- [ ] 14. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
