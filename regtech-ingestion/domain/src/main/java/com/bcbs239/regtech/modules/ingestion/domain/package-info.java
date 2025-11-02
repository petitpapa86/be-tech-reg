/**
 * Domain layer for the Ingestion module.
 * 
 * This package contains the core business logic and domain models for the ingestion functionality.
 * It follows Domain-Driven Design principles and is independent of external frameworks and infrastructure.
 * 
 * Key principles:
 * - No dependencies on external frameworks (except core utilities)
 * - Contains business logic and domain rules
 * - Defines repository interfaces (implemented in infrastructure layer)
 * - Contains domain events for cross-module communication
 * 
 * Package structure:
 * - batch: Contains IngestionBatch aggregate and related value objects
 * - bankinfo: Contains BankInfo entity and related domain logic
 * - performance: Contains performance-related domain models
 * - integrationevents: Contains events for cross-module communication
 */
package com.bcbs239.regtech.modules.ingestion.domain;