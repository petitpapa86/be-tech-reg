# Implementation Plan

- [x] 1. Create new capability package structure





  - Create `generation/`, `coordination/`, and `integration/` packages
  - Create `integration/events/` subdirectory for integration event classes
  - _Requirements: 1.1, 1.2_

- [x] 2. Move generation capability classes





  - [x] 2.1 Move orchestration classes to generation package


    - Move `ComprehensiveReportOrchestrator` to `generation/`
    - Move `ReportGenerationMetrics` to `generation/`
    - Move `IComprehensiveReportOrchestrator` interface to `coordination/` (it's used for coordination)
    - Update package declarations and imports
    - _Requirements: 1.1, 1.3_
  
  - [x] 2.2 Move aggregation classes to generation package


    - Move `ComprehensiveReportDataAggregator` to `generation/`
    - Move `ComprehensiveReportData` to `generation/`
    - Move `DataAggregationException` to `generation/`
    - Update package declarations and imports
    - _Requirements: 1.1, 1.3_
  
  - [x] 2.3 Move recommendations classes to generation package


    - Move `QualityRecommendationsGenerator` to `generation/`
    - Move `RecommendationSection` to `generation/`
    - Update package declarations and imports
    - _Requirements: 1.1, 1.3_

- [x] 3. Organize coordination capability




  - [x] 3.1 Keep coordination classes in place

    - Verify `ReportCoordinator` is in `coordination/`
    - Verify `BatchEventTracker` is in `coordination/`
    - Verify `CalculationEventData` is in `coordination/`
    - Verify `QualityEventData` is in `coordination/`
    - Move `IComprehensiveReportOrchestrator` from generation to `coordination/`
    - _Requirements: 1.4, 2.2_

- [x] 4. Move integration capability classes





  - [x] 4.1 Move event listener to integration package


    - Move `ReportEventListener` from `events/` to `integration/`
    - Update package declaration and imports
    - _Requirements: 1.1, 3.3_
  
  - [x] 4.2 Create integration event classes


    - Create `BatchCalculationCompletedIntegrationEvent` in `integration/events/`
    - Create `BatchQualityCompletedIntegrationEvent` in `integration/events/`
    - These should be self-contained DTOs for cross-module communication
    - _Requirements: 2.5, 3.3_

- [x] 5. Update all import statements






  - [x] 5.1 Update imports in generation package classes

    - Update imports in all classes moved to `generation/`
    - Ensure they reference correct packages for coordination and integration classes
    - _Requirements: 4.2_
  

  - [x] 5.2 Update imports in coordination package classes

    - Update imports in `ReportCoordinator` to reference new generation package
    - Update imports in other coordination classes as needed
    - _Requirements: 4.2_
  

  - [x] 5.3 Update imports in integration package classes

    - Update imports in `ReportEventListener` to reference new packages
    - _Requirements: 4.2_
  

  - [x] 5.4 Update imports in infrastructure layer

    - Update any infrastructure classes that reference application layer classes
    - _Requirements: 4.2_
  
  - [x] 5.5 Update imports in presentation layer


    - Update any presentation classes that reference application layer classes
    - _Requirements: 4.2_

- [x] 6. Update test imports






  - [x] 6.1 Find and update test class imports

    - Search for test files that import from old packages
    - Update imports to reference new package structure
    - _Requirements: 4.1, 4.2_

- [x] 7. Delete old empty packages










  - [x] 7.1 Remove old package directories



    - Delete `orchestration/` directory (should be empty)
    - Delete `aggregation/` directory (should be empty)
    - Delete `recommendations/` directory (should be empty)
    - Delete `events/` directory (should be empty)
    - _Requirements: 1.5_

- [x] 8. Verify compilation and tests






  - [x] 8.1 Compile the project

    - Run `mvn clean compile` on report-generation module
    - Fix any compilation errors
    - _Requirements: 4.2, 4.3_
  

  - [x] 8.2 Run all tests

    - Run `mvn test` on report-generation module
    - Verify all tests pass
    - _Requirements: 4.1_
  

  - [x] 8.3 Verify Spring context loads

    - Start the application
    - Verify no bean wiring errors
    - Check logs for warnings
    - _Requirements: 4.3, 4.4_

- [ ] 9. Create summary documentation
  - [ ] 9.1 Document the reorganization
    - Create `APPLICATION_LAYER_REORGANIZATION.md` in report-generation module root
    - Document the old vs new structure
    - Explain the business capabilities
    - Provide migration notes for other developers
    - _Requirements: 3.1, 3.4_
