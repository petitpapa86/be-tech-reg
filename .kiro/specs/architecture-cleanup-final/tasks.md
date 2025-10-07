# Implementation Plan

- [x] 1. Analyze and identify architecture violations





  - Scan domain packages for services, interfaces, and result objects
  - Check for existing application handlers that duplicate functionality
  - Map all dependencies and references that need updating
  - Create comprehensive cleanup plan with priority order
  - _Requirements: 1.1, 5.1, 5.2_

- [x] 2. Remove domain services and move to application handlers






  - [x] 2.1 Check TaxCalculationService against existing handlers

    - Examine `TaxCalculationService` in domain/tax package
    - Compare with existing `CalculateTaxHandler` in application layer
    - Remove domain service if handler already implements functionality
    - _Requirements: 1.1, 1.2, 1.3, 5.2, 5.3_


  - [x] 2.2 Remove TaxCalculationServiceInterface

    - Delete interface file from domain/tax package
    - Update any references to use functional closures instead
    - Create factory methods for tax calculation closures
    - _Requirements: 2.1, 2.2, 2.4, 7.1_

  - [x] 2.3 Scan for other domain services


    - Check all domain packages for remaining service classes
    - Verify no services ending with "Service" exist in domain
    - Remove or convert any additional services found
    - _Requirements: 1.1, 1.2_

- [x] 3. Eliminate interface dependencies with functional closures






  - [x] 3.1 Replace SimpleEmailService interface

    - Remove `SimpleEmailService` interface from domain/dunning
    - Update `DunningNotificationService` to use functional closures
    - Create `EmailRequest` record for functional composition
    - Add factory method for email sending closure
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 7.1_


  - [x] 3.2 Remove infrastructure SimpleEmailService

    - Delete `SimpleEmailService` from infrastructure/communication
    - Update any references to use functional approach
    - Ensure no interface dependencies remain
    - _Requirements: 2.1, 2.5_


  - [x] 3.3 Scan for other domain interfaces

    - Check all domain packages for service interfaces
    - Replace any remaining interfaces with functional closures
    - Update consuming classes to use Function types
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 4. Remove result objects from domain layer





  - [x] 4.1 Move LegalNoticeResult to application layer


    - Check if `LegalNoticeResult` is used by application handlers
    - Move to appropriate application package if needed
    - Remove from domain if only used internally
    - Update all import references
    - _Requirements: 3.1, 3.2, 3.4, 6.1_

  - [x] 4.2 Move CallScheduleResult to application layer


    - Check usage of `CallScheduleResult` in codebase
    - Move to application layer or remove if unused
    - Update references and imports
    - _Requirements: 3.1, 3.2, 3.4, 6.1_


  - [x] 4.3 Scan for other result objects in domain

    - Check all domain packages for result/response objects
    - Move application-specific results to application layer
    - Remove unused result objects
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. Update references and fix imports







  - [x] 5.1 Update all import statements

    - Fix imports for moved classes
    - Remove imports for deleted classes
    - Add imports for new functional types
    - _Requirements: 6.1, 6.2, 6.5_


  - [x] 5.2 Verify compilation success

    - Run compilation check after each major change
    - Fix any broken references or missing imports
    - Ensure no compilation errors remain
    - _Requirements: 6.2, 6.3_



  - [ ] 5.3 Update dependency injection configurations
    - Update Spring configurations for new functional closures
    - Remove configurations for deleted services
    - Add factory beans for closure creation
    - _Requirements: 6.1, 6.4_

- [ ] 6. Clean up empty directories and unused files
  - [ ] 6.1 Remove empty directories
    - Check for directories that became empty after cleanup
    - Remove empty subdirectories first, then parent directories
    - Ensure no empty directory structure remains
    - _Requirements: 1.5, 4.1, 4.2, 4.4_

  - [ ] 6.2 Clean up unused imports and references
    - Remove unused import statements
    - Clean up any orphaned references
    - Verify no dead code remains
    - _Requirements: 6.4, 6.5_

- [ ] 7. Validate architecture compliance
  - [ ] 7.1 Run architecture compliance tests
    - Verify no domain services remain
    - Check that all interfaces are eliminated
    - Confirm result objects are in correct layers
    - _Requirements: 1.1, 2.1, 3.1_

  - [ ] 7.2 Verify functional patterns
    - Ensure all closures follow functional principles
    - Check that handlers use immutable data structures
    - Validate Result pattern usage consistency
    - _Requirements: 7.1, 7.2, 7.4, 7.5_

  - [ ] 7.3 Final compilation and test validation
    - Run full compilation check
    - Execute all tests to ensure functionality preserved
    - Verify no regressions introduced
    - _Requirements: 6.2, 6.3_