# Bank Registry Context - Implementation Plan

## Implementation Tasks

- [ ] 1. Create Italian banking value objects with validation
  - Implement AbiCode record with 5-digit pattern validation
  - Create LeiCode record with 20-character alphanumeric validation
  - Implement DenominazioneSociale record with length constraints
  - Create SedeLegale record with Italian address validation (CAP, provincia)
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [ ] 2. Build Bank aggregate with comprehensive Italian banking information
  - Create Bank record with all Italian banking fields (denominazione sociale, gruppo bancario, tipologia banca, categoria vigilanza)
  - Add factory method Bank.create() with validation and status management
  - Implement markAsValidated() method for external registry confirmation
  - Create isConfigurationComplete() method for multi-stage validation
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 2.1_

- [ ] 3. Implement BCBS 239 parameters aggregate with regulatory compliance
  - Create BankParameters record with limite grande esposizione and soglia classificazione
  - Implement CapitaleAmmissibile record with minimum capital validation (€1M+)
  - Add LimiteGrandeEsposizione record with 25% EU CRR limit validation
  - Create SogliaQualitaMinima record with 95% minimum threshold
  - Implement isCompliant() method for regulatory threshold checking
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4. Build multi-template regulatory configuration system
  - Create TemplateId enum with three supported templates (IT_LARGE_EXPOSURES_CIRCULARE_285, EU_LARGE_EXPOSURES_EBA_ITS, IT_RISK_CONCENTRATION_SUPERVISORY)
  - Implement BankTemplateConfiguration aggregate for managing multiple templates per bank
  - Add TemplateConfiguration record for individual template settings with authority-specific submission configurations
  - Create RegulatoryTemplate.loadTemplate() method that loads YAML configurations from classpath
  - Implement template-specific section mapping for autonomous context data ownership
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 5. Create pure business functions for bank registration
  - Implement BankRegistrationService.registerBank() pure function
  - Add closure-based external validation for GLEIF and Banca d'Italia registries
  - Create validateBankNotExists() function with ABI code uniqueness checking
  - Implement validateExternalRegistries() function for LEI and ABI validation
  - Add BankRegistrationResponse with nextStep guidance ("BCBS_PARAMETERS")
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 7.2_

- [ ] 6. Implement BCBS 239 parameters configuration function
  - Create BcbsParametersService.configureBcbsParameters() pure function
  - Add validation for regulatory compliance (25% limit, 10% threshold, 95% quality)
  - Implement capital adequacy validation with minimum requirements
  - Create BcbsParametersResponse with nextStep guidance ("REPORT_CONFIG")
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 7. Build multi-template configuration function with regulatory processing
  - Implement TemplateConfigurationService.configureMultipleTemplates() pure function
  - Add support for configuring multiple regulatory templates simultaneously
  - Create template-specific validation for Italian, EU, and supervisory requirements
  - Implement authority-specific submission configuration (Banca d'Italia, EBA, ECB)
  - Add MultiTemplateConfigResponse with template activation status
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 8. Create thin event publishing system for multi-template downstream coordination
  - Implement BankRegisteredEvent with minimal data (bankId, abiCode, leiCode, timestamp)
  - Create BankParametersConfiguredEvent with capital amount and parameters ID
  - Add MultiTemplateConfiguredEvent with configured template set and context section mapping
  - Implement template-specific section ownership mapping for autonomous context coordination
  - Create EventPublisher interface with support for multiple regulatory frameworks
  - _Requirements: 2.5, 4.5, 6.5, 7.5_

- [ ] 9. Build query API system for downstream context access
  - Create closure-based bankSummaryQuery function for cross-context data access
  - Implement bankParametersQuery function for capital data requirements
  - Add bankByAbiQuery function for exposure validation needs
  - Create BankSummary and CapitalInfo DTOs for lightweight cross-context communication
  - _Requirements: 2.4, 4.1, 5.1, 5.2_

- [ ] 10. Implement BankConfigReactor for multi-template Service Composer orchestration
  - Create BankConfigReactor handling multi-stage configuration (BANK_INSTITUTION, BCBS_PARAMETERS, TEMPLATE_CONFIG)
  - Add support for configuring multiple regulatory templates in single operation
  - Implement handleBankInstitutionConfig() with BankRegisteredEvent publishing
  - Create handleBcbsParametersConfig() with BankParametersConfiguredEvent publishing
  - Add handleMultiTemplateConfig() with MultiTemplateConfiguredEvent and template-specific section mapping
  - _Requirements: 1.1, 1.4, 4.1, 4.5, 6.1, 6.5, 7.1, 7.5_

- [ ] 11. Create BankInfoReactor for downstream context data provision
  - Implement BankInfoReactor for GET operations on /dashboard, /exposures, /reports routes
  - Add bank summary provision for dashboard and report contexts
  - Create capital info provision for exposure calculation contexts
  - Implement tenant context validation for bank access control
  - _Requirements: 5.1, 5.2, 5.3, 8.1, 8.2_

- [ ] 12. Build BankValidationReactor for external registry integration
  - Create BankValidationReactor for external LEI and ABI validation
  - Implement GLEIF registry integration for LEI code verification
  - Add Banca d'Italia registry integration for ABI code validation
  - Create BankValidationResult with detailed validation status
  - Publish BankValidationCompletedEvent for downstream notification
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. Implement repository interfaces with autonomous data ownership
  - Create BankRepository interface with closure-based method signatures
  - Add BankParametersRepository interface for BCBS parameters storage
  - Implement ReportConfigRepository interface for template configurations
  - Create repository factory functions for dependency injection
  - Ensure no foreign keys to other schemas - enforce bank_id references only
  - _Requirements: 2.4, 2.5, 8.1, 8.2, 8.3_

- [ ] 14. Create external registry integration services
  - Implement GleifRegistryService for LEI code validation with closure-based design
  - Add BancaItaliaRegistryService for ABI code verification
  - Create StripeIntegrationService for subscription management
  - Implement retry logic and circuit breaker patterns for external calls
  - _Requirements: 7.1, 7.2, 7.3, 9.1, 9.2_

- [ ] 15. Build subscription tier management with multi-bank limits
  - Create SubscriptionTier enum with bank limits (STARTER: 1, PROFESSIONAL: 5, ENTERPRISE: unlimited)
  - Implement subscription limit validation before bank registration
  - Add tier upgrade suggestion logic when limits are approached
  - Create subscription billing integration with Stripe webhooks
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 9.1, 9.5_

- [ ] 16. Implement comprehensive error handling for Italian banking
  - Create BankRegistryErrorCodes constants for domain-specific errors
  - Add Italian banking validation error messages with proper localization
  - Implement external registry failure handling with fallback mechanisms
  - Create subscription limit exceeded error handling with upgrade suggestions
  - _Requirements: 1.3, 7.3, 7.4, 3.3, 3.4_

- [ ] 17. Create JPA entities with schema isolation
  - Implement BankEntity in bank_registry schema with no foreign keys to other schemas
  - Add BankParametersEntity with proper decimal precision for financial amounts
  - Create ReportConfigEntity with JSONB storage for template configurations
  - Implement entity-to-domain model mapping with validation
  - Ensure strict schema isolation - other contexts store only bank_id references
  - _Requirements: 2.4, 8.1, 8.2, 8.3, 8.4_

- [ ] 18. Build Spring Boot configuration and auto-configuration
  - Create BankRegistryConfiguration with external registry settings
  - Add conditional bean creation for GLEIF and Banca d'Italia integrations
  - Implement health check endpoints for external registry connectivity
  - Create configuration properties for Italian banking validation rules
  - _Requirements: 7.1, 7.2, 7.5, 8.1, 8.2_

- [ ] 19. Implement REST endpoints with Italian banking compliance
  - Create BankController with multi-stage configuration endpoints
  - Add BankValidationController for external registry validation
  - Implement proper HTTP status codes and Italian error messages
  - Create OpenAPI documentation with Italian banking examples
  - _Requirements: 1.1, 1.4, 7.1, 7.2, 10.1_

- [ ] 20. Create database migration scripts for bank_registry schema
  - Implement V001__create_bank_registry_schema.sql with proper Italian banking fields
  - Add V002__create_bank_parameters_table.sql with decimal precision for financial data
  - Create V003__create_report_config_table.sql with JSONB for template storage
  - Add proper indexes for ABI codes, LEI codes, and bank lookups
  - Ensure no foreign key constraints to other schemas for autonomous operation
  - _Requirements: 2.4, 8.1, 8.2, 8.3, 10.2_