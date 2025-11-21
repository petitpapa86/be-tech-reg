# Design Document

## Overview

This design reorganizes the report-generation application layer from a technical layer structure to a business capability structure. The current organization by technical concerns (orchestration, aggregation, recommendations, coordination, events) will be replaced with organization by business capabilities (generation, coordination, integration).

## Architecture

### Current Structure (Technical Layers - BAD)
```
application/
├── orchestration/          # Technical layer
│   ├── ComprehensiveReportOrchestrator
│   └── ReportGenerationMetrics
├── aggregation/            # Technical layer
│   ├── ComprehensiveReportDataAggregator
│   └── ComprehensiveReportData
├── recommendations/        # Technical layer
│   ├── QualityRecommendationsGenerator
│   └── RecommendationSection
├── coordination/           # Technical layer
│   ├── ReportCoordinator
│   ├── BatchEventTracker
│   ├── CalculationEventData
│   └── QualityEventData
└── events/                 # Technical layer
    └── ReportEventListener
```

### Proposed Structure (Business Capabilities - GOOD)
```
application/
├── generation/             # Business capability: Generate comprehensive reports
│   ├── GenerateComprehensiveReportCommand
│   ├── GenerateComprehensiveReportCommandHandler
│   ├── ComprehensiveReportOrchestrator
│   ├── ComprehensiveReportDataAggregator
│   ├── ComprehensiveReportData
│   ├── QualityRecommendationsGenerator
│   ├── RecommendationSection
│   ├── ReportGenerationMetrics
│   └── DataAggregationException
├── coordination/           # Business capability: Coordinate batch events
│   ├── ReportCoordinator
│   ├── BatchEventTracker
│   ├── CalculationEventData
│   ├── QualityEventData
│   └── IComprehensiveReportOrchestrator (interface)
└── integration/            # Business capability: Handle integration events
    ├── events/
    │   ├── BatchCalculationCompletedIntegrationEvent
    │   └── BatchQualityCompletedIntegrationEvent
    └── ReportEventListener
```

## Components and Interfaces

### Generation Capability

**Purpose**: Orchestrate the generation of comprehensive reports by aggregating data from multiple sources and generating recommendations.

**Key Components**:
- `GenerateComprehensiveReportCommand`: Command object containing batch context
- `GenerateComprehensiveReportCommandHandler`: Handles the command and orchestrates report generation
- `ComprehensiveReportOrchestrator`: Orchestrates the report generation workflow
- `ComprehensiveReportDataAggregator`: Aggregates data from risk calculation and quality modules
- `QualityRecommendationsGenerator`: Generates actionable recommendations based on quality metrics
- `ReportGenerationMetrics`: Tracks metrics for report generation operations

**Responsibilities**:
- Aggregate data from multiple sources (risk calculation, data quality)
- Generate HTML and XBRL reports
- Generate quality recommendations
- Store reports in S3
- Persist report metadata
- Track generation metrics

### Coordination Capability

**Purpose**: Coordinate batch processing events across modules to trigger report generation when all prerequisites are met.

**Key Components**:
- `ReportCoordinator`: Coordinates batch events and triggers report generation
- `BatchEventTracker`: Tracks which events have been received for each batch
- `CalculationEventData`: Data from risk calculation completion events
- `QualityEventData`: Data from data quality completion events
- `IComprehensiveReportOrchestrator`: Interface for triggering report generation

**Responsibilities**:
- Track batch calculation completion events
- Track batch quality completion events
- Determine when all prerequisites are met
- Trigger comprehensive report generation
- Handle event expiration and cleanup

### Integration Capability

**Purpose**: Handle integration events from other bounded contexts (risk-calculation, data-quality).

**Key Components**:
- `ReportEventListener`: Listens for integration events from other modules
- `BatchCalculationCompletedIntegrationEvent`: Event from risk-calculation module
- `BatchQualityCompletedIntegrationEvent`: Event from data-quality module

**Responsibilities**:
- Listen for batch calculation completed events
- Listen for batch quality completed events
- Validate event data
- Forward events to coordination capability
- Handle duplicate events
- Log integration events

## Data Models

### Command Objects

```java
// generation/GenerateComprehensiveReportCommand.java
public class GenerateComprehensiveReportCommand {
    private final BatchContext batchContext;
    private final CalculationEventData calculationData;
    private final QualityEventData qualityData;
}
```

### Event Data Objects

```java
// coordination/CalculationEventData.java
public class CalculationEventData {
    private final String batchId;
    private final String bankId;
    private final Instant completedAt;
    // ... other fields
}

// coordination/QualityEventData.java  
public class QualityEventData {
    private final String batchId;
    private final String bankId;
    private final Instant completedAt;
    // ... other fields
}
```

### Integration Events

```java
// integration/events/BatchCalculationCompletedIntegrationEvent.java
public class BatchCalculationCompletedIntegrationEvent {
    private final String batchId;
    private final String bankId;
    // ... fields from risk-calculation module
}

// integration/events/BatchQualityCompletedIntegrationEvent.java
public class BatchQualityCompletedIntegrationEvent {
    private final String batchId;
    private final String bankId;
    // ... fields from data-quality module
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Package organization reflects business capabilities
*For any* class in the application layer, its package name should reflect the business capability it belongs to (generation, coordination, or integration), not a technical layer name
**Validates: Requirements 1.1, 1.2, 1.5**

### Property 2: Capability isolation
*For any* modification to a capability package, classes in other capability packages should not require changes unless the interface contract changes
**Validates: Requirements 2.1, 2.2, 2.4**

### Property 3: Co-location of related classes
*For any* business capability, all related commands, handlers, services, DTOs, and exceptions should be located in the same capability package
**Validates: Requirements 3.2**

### Property 4: Functional preservation
*For any* existing test, it should pass after reorganization with only import statement updates
**Validates: Requirements 4.1, 4.2**

### Property 5: Dependency injection preservation
*For any* Spring bean, it should be properly wired and functional after relocation
**Validates: Requirements 4.3, 4.4**

## Error Handling

### Migration Errors

**Compilation Errors**: If imports are not updated correctly, compilation will fail. Solution: Use IDE refactoring tools or systematic find-replace.

**Bean Wiring Errors**: If Spring cannot find beans after relocation, application startup will fail. Solution: Ensure all classes maintain their `@Component`, `@Service`, etc. annotations.

**Test Failures**: If tests reference old package names, they will fail. Solution: Update test imports systematically.

## Testing Strategy

### Unit Tests

- Verify that all existing unit tests pass after reorganization
- Update test imports to reflect new package structure
- No changes to test logic should be required

### Integration Tests

- Verify that Spring context loads correctly with new package structure
- Verify that event listeners receive and process events correctly
- Verify that report generation workflow completes successfully

### Manual Verification

- Start the application and verify no bean wiring errors
- Trigger a batch processing workflow end-to-end
- Verify that reports are generated correctly
- Check logs for any errors or warnings

## Migration Strategy

### Phase 1: Create New Package Structure
1. Create new capability packages: `generation/`, `coordination/`, `integration/`
2. Create subdirectories as needed (e.g., `integration/events/`)

### Phase 2: Move Classes
1. Move classes to appropriate capability packages:
   - `orchestration/*` → `generation/`
   - `aggregation/*` → `generation/`
   - `recommendations/*` → `generation/`
   - `coordination/*` → `coordination/` (keep as-is, already correct)
   - `events/*` → `integration/`

### Phase 3: Update Imports
1. Update all import statements in moved classes
2. Update all import statements in classes that reference moved classes
3. Update test imports

### Phase 4: Create Missing Artifacts
1. Create `GenerateComprehensiveReportCommand` if not exists
2. Create `GenerateComprehensiveReportCommandHandler` if not exists
3. Create integration event classes if not exists

### Phase 5: Verify
1. Compile the project
2. Run all tests
3. Start the application
4. Verify end-to-end functionality

## Benefits

1. **Improved Discoverability**: Developers can immediately understand what the system does by looking at package names
2. **Better Cohesion**: Related classes are grouped together, making changes easier
3. **Reduced Coupling**: Clear boundaries between capabilities reduce unintended dependencies
4. **Consistency**: Matches the organizational pattern used in other modules
5. **Maintainability**: Easier to onboard new developers and maintain the codebase
