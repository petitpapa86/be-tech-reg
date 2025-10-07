# Implementation Plan

- [x] 1. Create core functional repository infrastructure





  - Create base functional interfaces and patterns for repository operations
  - Implement Result pattern integration for functional repository operations
  - _Requirements: 1.1, 6.1, 6.2_


- [x] 1.1 Define functional repository base interfaces

  - Write base functional interfaces that all repositories will implement
  - Create standard function signatures for common repository operations (save, find, delete)
  - _Requirements: 1.1, 6.1_



- [x] 1.2 Create Result pattern integration utilities





  - Implement utility functions for composing Result types with repository functions
  - Write helper methods for common functional composition patterns
  - _Requirements: 1.4, 8.4_

- [x] 2. Implement payment repository functional pattern





  - Convert PaymentRepository and related repositories to use functional composition pattern
  - Create PaymentRepositoryFunctions aggregator class
  - _Requirements: 1.1, 3.1, 3.2_

- [x] 2.1 Create PaymentRepositoryFunctions aggregator


  - Write PaymentRepositoryFunctions class that aggregates payment-related repositories
  - Implement getter methods for PaymentRepository, PaymentAttemptRepository, and PaymentValidationRepository
  - _Requirements: 3.1, 3.2, 8.1_


- [x] 2.2 Convert PaymentRepository to functional interface

  - Update PaymentRepository interface to return Function types instead of direct results
  - Implement function-returning methods for save, find, and query operations
  - _Requirements: 1.1, 1.2, 6.1_



- [x] 2.3 Update PaymentRepositoryImpl with functional implementation





  - Implement functional methods in PaymentRepositoryImpl that return Function instances
  - Ensure proper error handling and logging within function implementations


  - _Requirements: 1.1, 5.3, 7.1_

- [x] 2.4 Convert PaymentAttemptRepository to functional pattern





  - Update PaymentAttemptRepository interface and implementation to use functional pattern
  - Implement function-returning methods for payment attempt operations
  - _Requirements: 1.1, 1.2_

- [x] 3. Refactor payment handlers to use functional composition





  - Update ProcessPaymentHandler and RetryPaymentHandler to use direct repository injection
  - Replace repository aggregators with direct functional repository access

  - _Requirements: 1.1, 1.3, 2.1_

- [x] 3.1 Update ProcessPaymentHandler functional composition

  - Inject PaymentRepositoryImpl and PaymentAttemptRepositoryImpl directly
  - Use repository.operation().apply() pattern for clean functional chains
  - _Requirements: 1.1, 1.3, 2.3_



- [ ] 3.2 Update RetryPaymentHandler functional composition
  - Convert retry payment logic to use direct repository injection
  - Implement method reference usage where appropriate for cleaner code
  - _Requirements: 1.1, 4.1, 4.2_

- [x] 4. Implement subscription repository functional pattern





  - Convert SubscriptionRepositoryImpl to functional pattern (no interface needed)
  - Update related repository implementations to use functional methods
  - _Requirements: 1.1, 3.1, 3.2_

- [x] 4.1 Convert SubscriptionRepositoryImpl to functional pattern


  - Update SubscriptionRepositoryImpl to return Function types directly
  - Remove interface dependency and use direct injection pattern
  - _Requirements: 1.1, 1.2, 6.1_


- [x] 4.2 Update SubscriptionRepositoryImpl with functional implementation

  - Implement functional methods that return Function<Input, Result<Output, BillingError>>
  - Ensure integration with existing JPA repositories and transaction management
  - _Requirements: 1.1, 8.2, 8.3_

- [x] 4.3 Convert subscription-related repositories to functional pattern


  - Update TierRecommendationRepositoryImpl and PendingTierChangeRepositoryImpl to functional pattern
  - Implement function-returning methods for tier management operations (no interfaces)
  - _Requirements: 1.1, 1.2_

- [x] 5. Refactor subscription handlers to use functional composition





  - Update subscription handlers to use direct repository injection
  - Replace aggregator dependencies with direct functional repository access
  - _Requirements: 1.1, 2.1, 2.2_

- [x] 5.1 Update CreateSubscriptionHandler functional composition


  - Inject SubscriptionRepositoryImpl directly instead of using aggregators
  - Use repository.operation().apply() pattern for clean functional chains
  - _Requirements: 1.1, 2.1, 2.2_


- [x] 5.2 Update UpgradeTierHandler functional composition

  - Implement direct repository injection for tier upgrade operations
  - Use method references for cleaner functional code where appropriate
  - _Requirements: 1.1, 4.1, 4.2_



- [ ] 5.3 Update CancelSubscriptionHandler functional composition
  - Convert subscription cancellation logic to use direct repository injection
  - Ensure proper error handling through Result pattern composition
  - _Requirements: 1.1, 1.4, 5.2_

- [x] 6. Implement invoice repository functional pattern





  - Convert InvoiceRepositoryImpl to functional pattern (no interface needed)
  - Update related repository implementations to use functional methods
  - _Requirements: 1.1, 3.1, 3.2_


- [x] 6.1 Convert InvoiceRepositoryImpl to functional pattern

  - Update InvoiceRepositoryImpl to return Function types directly
  - Remove interface dependency and use direct injection pattern
  - _Requirements: 1.1, 1.2, 6.1_


- [x] 6.2 Update InvoiceRepositoryImpl with functional implementation

  - Implement functional methods that return Function<Input, Result<Output, BillingError>>
  - Maintain compatibility with existing transaction and security constraints
  - _Requirements: 1.1, 8.2, 8.3_

- [-] 7. Refactor invoice handlers to use functional composition



  - Update invoice handlers to use direct repository injection
  - Replace aggregator dependencies with direct functional repository access
  - _Requirements: 1.1, 2.1, 2.2_

- [x] 7.1 Update GenerateInvoiceHandler functional composition


  - Inject InvoiceRepositoryImpl directly instead of using aggregators
  - Use repository.operation().apply() pattern for clean functional chains
  - _Requirements: 1.1, 1.3, 2.3_

- [x] 7.2 Update invoice query handlers functional composition





  - Convert GetInvoiceHandler and ListInvoicesHandler to use direct repository injection
  - Use method references for query operations where appropriate
  - _Requirements: 1.1, 4.1, 4.2_

- [x] 8. Implement account management repository functional pattern





  - Convert BillingAccountRepositoryImpl to functional pattern (no interface needed)
  - Update implementation to use functional methods
  - _Requirements: 1.1, 3.1, 3.2_

- [x] 8.1 Convert BillingAccountRepositoryImpl to functional pattern


  - Update BillingAccountRepositoryImpl to return Function types directly
  - Remove interface dependency and use direct injection pattern
  - _Requirements: 1.1, 1.2, 6.1_


- [x] 8.2 Update BillingAccountRepositoryImpl with functional implementation

  - Implement functional methods that return Function<Input, Result<Output, BillingError>>
  - Ensure proper integration with existing infrastructure
  - _Requirements: 1.1, 8.1, 8.2_

- [x] 9. Refactor account management handlers to use functional composition





  - Update account management handlers to use direct repository injection
  - Replace aggregator dependencies with direct functional repository access
  - _Requirements: 1.1, 2.1, 2.2_

- [x] 9.1 Update CreateBillingAccountHandler functional composition


  - Inject BillingAccountRepositoryImpl directly instead of using aggregators
  - Use repository.operation().apply() pattern for clean functional chains
  - _Requirements: 1.1, 2.1, 2.2_


- [x] 9.2 Update account lifecycle handlers functional composition

  - Convert UpdateAccountHandler, SuspendAccountHandler, and ReactivateAccountHandler to use direct repository injection
  - Implement method reference usage for cleaner functional composition
  - _Requirements: 1.1, 4.1, 4.2_

- [ ] 10. Create comprehensive test suite for functional repository pattern
  - Write unit tests for functional repository implementations
  - Create integration tests for functional composition in handlers
  - _Requirements: 6.1, 6.2, 7.3_

- [ ] 10.1 Write unit tests for repository function implementations
  - Create unit tests that verify individual repository functions work correctly
  - Test error handling and Result pattern integration in repository functions
  - _Requirements: 6.1, 6.2_

- [ ] 10.2 Write integration tests for functional composition
  - Create integration tests that verify complete functional composition chains
  - Test method reference usage and functional pipeline behavior
  - _Requirements: 4.1, 4.2, 7.3_

- [ ]* 10.3 Write performance tests for functional pattern
  - Create performance tests comparing functional pattern to traditional repository access
  - Verify that functional composition doesn't introduce significant overhead
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 11. Remove legacy aggregator classes and wrapper methods





  - Clean up old repository aggregator classes that are no longer needed
  - Remove wrapper methods that have been replaced by direct injection
  - _Requirements: 2.1, 2.2, 5.1_


- [x] 11.1 Remove repository aggregator classes

  - Delete PaymentRepositoryFunctions, SubscriptionRepositoryFunctions, and other aggregator classes
  - Update dependency injection to use direct repository implementation injection
  - _Requirements: 2.1, 2.2_


- [x] 11.2 Clean up unused wrapper methods

  - Remove any remaining wrapper methods that delegate to repository aggregators
  - Ensure all handlers use direct repository injection pattern
  - _Requirements: 2.2, 5.1_

- [x] 12. Update documentation and migration guide




  - Create documentation for the correct functional repository pattern
  - Write migration guide showing direct injection approach
  - _Requirements: 5.1, 5.2_

- [x] 12.1 Write functional repository pattern documentation


  - Document the no-interface, direct injection pattern with usage examples
  - Show how to use repository.operation().apply() for clean functional chains
  - _Requirements: 5.1, 5.2_

- [x] 12.2 Create developer migration guide


  - Write step-by-step guide for migrating from aggregators to direct injection
  - Document best practices for functional repository composition without interfaces
  - _Requirements: 5.1, 5.2_