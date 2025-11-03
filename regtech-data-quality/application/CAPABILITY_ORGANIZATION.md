# Data Quality Application Layer - Capability-Based Organization

## Overview
The application layer is organized by business capabilities rather than technical patterns (commands/queries). This approach provides better cohesion and makes the codebase more maintainable.

## Capability Structure

### 1. Validation Capability
**Purpose**: Handles all quality validation workflows and processing
- Commands: ValidateBatchQualityCommand, ValidateExposureCommand
- Handlers: ValidateBatchQualityCommandHandler
- Services: QualityValidationEngine
- DTOs: ValidationSummaryDto

### 2. Reporting Capability  
**Purpose**: Manages quality reports and report generation
- Queries: GetQualityReportQuery, GetQualityReportSummaryQuery
- Handlers: QualityReportQueryHandler
- DTOs: QualityReportDto, QualityReportSummaryDto

### 3. Scoring Capability
**Purpose**: Handles quality scoring, grading, and calculations
- Services: QualityScoringEngine
- DTOs: QualityScoresDto

### 4. Monitoring Capability
**Purpose**: Provides quality trends, analytics, and monitoring
- Queries: BatchQualityTrendsQuery
- Handlers: BatchQualityTrendsQueryHandler  
- DTOs: QualityTrendsDto

### 5. Integration Capability
**Purpose**: Handles cross-module integration and external services
- Services: CrossModuleEventPublisher, S3StorageService

## Benefits
- **Business Alignment**: Structure reflects business capabilities
- **Cohesion**: Related functionality is grouped together
- **Maintainability**: Easier to locate and modify related code
- **Team Organization**: Teams can own specific capabilities
- **Evolution**: Capabilities can evolve independently

## Migration Strategy
1. Create new capability-based structure
2. Move existing files to appropriate capabilities
3. Update imports and references
4. Remove old structure