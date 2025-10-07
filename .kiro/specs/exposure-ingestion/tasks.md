# Exposure Ingestion Context - Implementation Plan

## Implementation Tasks

- [ ] 1. Create core domain aggregates with template awareness
  - Implement ExposureBatch aggregate with template ID, processing metrics, and quality assessment
  - Create RawExposure aggregate with template-specific data validation
  - Add ProcessingError aggregate with severity categorization and row-level error tracking
  - Implement UserPermission aggregate for autonomous permission caching
  - _Requirements: 1.3, 1.4, 5.1, 5.2, 11.4_

- [ ] 2. Build template processing system with regulatory framework support
  - Create TemplateProcessor interface with support for 3 regulatory templates (Italian Circolare 285, EU EBA ITS, Italian Risk Concentration)
  - Implement ItalianLargeExposureValidator for Circolare 285 template validation
  - Add EbaLargeExposureValidator for EU EBA ITS template validation
  - Create SupervisoryRiskValidator for Italian Risk Concentration template validation
  - Implement template-specific business rule validation and quality scoring
  - _Requirements: 2.1, 2.3, 2.5, 12.1, 12.3_

- [ ] 3. Implement file parsing system with multi-format support
  - Create FileParser interface with Excel (.xlsx, .xls) and CSV format support
  - Add ExcelFileParser with automatic sheet detection and data range identification
  - Implement CsvFileParser with delimiter detection and encoding handling
  - Create FileFormat enum and FileTypeDetector for automatic format identification
  - Add comprehensive parsing error handling with row-by-row error reporting
  - _Requirements: 4.1, 4.2, 4.4, 4.5, 6.2_

- [ ] 4. Build Service Composer Framework integration with proper patterns
  - Create FileUploadComposer as primary data owner for file processing workflow
  - Implement FileUploadPermissionReactor (order=0) for autonomous permission validation
  - Add BillingUsageReactor (order=2) for usage tracking coordination
  - Create RiskCalculationReactor (order=3) for exposure event publishing
  - Implement DataQualityReactor (order=4) for quality assessment coordination
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 5. Create dashboard management system with Italian localization
  - Implement ExposureDashboardComposer as primary data owner for dashboard data
  - Add BankInfoReactor (order=2) for bank context enrichment
  - Create DashboardMetricsCalculator for summary statistics (total files, success percentage, processing time estimates)
  - Implement Italian UI labels (Nome File, Data Caricamento, Stato, Qualità, Conformità, Violazioni, Azioni)
  - Add real-time processing status with estimated completion times
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6. Implement autonomous permission validation system
  - Create UserPermissionRepository for local permission storage in exposure_ingestion schema
  - Add UserPermissionEventHandler for UserRoleAssignedEvent processing
  - Implement role-based upload validation (COMPLIANCE_OFFICER, RISK_MANAGER, DATA_ANALYST can upload; VIEWER cannot)
  - Create permission caching mechanism without cross-context queries
  - Add permission validation error handling with clear role-based error messages
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 7. Build drag-and-drop file upload interface with template selection
  - Create file upload UI with Italian text "Trascina qui i tuoi file o clicca per selezionare"
  - Implement template selection dropdown with 3 regulatory options before upload
  - Add file format validation for Excel (.xlsx, .xls) and CSV (.csv) formats
  - Create real-time upload progress tracking with percentage completion
  - Implement file size validation (100MB limit) and format rejection handling
  - _Requirements: 2.1, 2.2, 2.4, 3.1, 3.2_

- [ ] 8. Create comprehensive search and filtering system
  - Implement advanced search by file name, upload date range, processing status, and template type
  - Add period filtering with dropdown options (today, last week, last month, custom range)
  - Create status filtering by Completati, In Elaborazione, Con Errori, and Conforme states
  - Implement export functionality for filtered file lists to Excel and CSV formats
  - Add column sorting capabilities with ascending/descending options
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 9. Implement cloud storage integration with Google Cloud Storage
  - Create CloudStorageService for secure file storage with encryption
  - Add file organization by bank_id and date for easy retrieval and tenant isolation
  - Implement signed URLs for secure file access and download capabilities
  - Create retention policies for automatic archiving per regulatory requirements
  - Add disaster recovery with redundant storage across multiple regions
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 10. Build real-time processing feedback system
  - Create ProcessingMetrics value object with progress tracking and time estimation
  - Implement real-time progress updates during file parsing and validation phases
  - Add processing phase indicators (upload, parsing, validation, storage)
  - Create record counters for total, processed, and error records with live updates
  - Implement estimated completion time calculation based on processing rate
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 11. Create thin event publishing system for autonomous coordination
  - Implement ExposureIngestedEvent with minimal data (exposureId, batchId, bankId, grossAmount, currency)
  - Add BatchProcessedEvent with summary data (batchId, bankId, templateId, totalExposures, qualityScore)
  - Create FileProcessedEvent for billing coordination (batchId, bankId, recordCount)
  - Implement EventPublisher interface for autonomous context coordination
  - Add event publishing in reactors without fat event data to maintain bounded context autonomy
  - _Requirements: 11.1, 11.2, 11.3, 11.5_

- [ ] 12. Implement data quality pre-screening with BCBS 239 compliance
  - Create QualityAssessment value object with data quality and compliance scoring
  - Add template-specific mandatory field validation based on regulatory framework
  - Implement BCBS 239 compliance percentage calculation using 4 data quality principles
  - Create violation detection and counting specific to selected template
  - Add quality scoring with separate scores for data quality and regulatory compliance
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 13. Build error handling and recovery system
  - Create ProcessingError aggregate with WARNING, ERROR, and CRITICAL severity levels
  - Implement detailed error reporting with row-by-row breakdown and field-level details
  - Add partial failure handling to process valid records while flagging problematic ones
  - Create file corruption detection with original file preservation for analysis
  - Implement batch reprocessing capabilities for failed batches with corrections
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 14. Create exposure record creation system with data lineage
  - Implement unique exposure_id generation for each record with proper traceability
  - Add batch linking to connect all exposures to their originating batch_id
  - Create comprehensive data lineage tracking (file source, processing timestamp, user information)
  - Implement currency conversion handling with original amounts and FX rate storage
  - Add bank_id isolation for multi-tenant security and data separation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 15. Implement repository interfaces with autonomous data ownership
  - Create ExposureBatchRepository for batch management in exposure_ingestion schema
  - Add RawExposureRepository for exposure data storage with no foreign keys to other schemas
  - Implement ProcessingErrorRepository for error tracking and analysis
  - Create UserPermissionRepository for local permission caching
  - Add repository methods for dashboard queries, search, and filtering operations
  - _Requirements: 11.4, 11.5, 7.1, 9.1_

- [ ] 16. Build JPA entities with schema isolation
  - Create ExposureBatchEntity in exposure_ingestion schema with proper field mapping
  - Implement RawExposureEntity with template-specific data storage (JSONB for flexibility)
  - Add ProcessingErrorEntity with error categorization and severity tracking
  - Create UserPermissionEntity for autonomous permission storage
  - Ensure strict schema isolation with no foreign keys to other bounded contexts
  - _Requirements: 11.4, 11.5, 1.3, 5.5_

- [ ] 17. Create REST endpoints with Italian localization
  - Implement FileUploadController with template selection and drag-and-drop support
  - Add DashboardController with Italian labels and comprehensive file management
  - Create SearchController with advanced filtering and export capabilities
  - Implement proper HTTP status codes and Italian error messages
  - Add OpenAPI documentation with Italian banking examples and template specifications
  - _Requirements: 2.1, 7.1, 9.1, 2.4_

- [ ] 18. Implement Spring Boot configuration and integration
  - Create ExposureIngestionConfiguration with template processing settings
  - Add conditional bean creation for different template validators
  - Implement health check endpoints for file processing and cloud storage connectivity
  - Create configuration properties for upload limits, processing timeouts, and quality thresholds
  - Add integration with Service Composer Framework for handler registration
  - _Requirements: 2.5, 8.1, 3.5, 12.5_

- [ ] 19. Build database migration scripts for exposure_ingestion schema
  - Create V001__create_exposure_ingestion_schema.sql with proper table structure
  - Add V002__create_ingestion_batches_table.sql with processing metrics and quality assessment fields
  - Implement V003__create_raw_exposures_table.sql with JSONB for template-specific data
  - Create V004__create_processing_errors_table.sql with severity and error categorization
  - Add V005__create_user_permissions_table.sql for autonomous permission caching
  - _Requirements: 11.4, 1.3, 5.1, 6.1_

- [ ] 20. Create comprehensive dashboard UI components
  - Implement file management dashboard with summary statistics and Italian labels
  - Add file list table with sortable columns (Nome File, Data Caricamento, Record, Stato, Qualità, Conformità, Violazioni)
  - Create search and filter UI with period selection and status filtering
  - Implement file actions (view details, download, export) with proper permission checking
  - Add real-time updates for processing status and estimated completion times
  - _Requirements: 7.1, 7.2, 7.3, 7.5, 9.1, 9.5_