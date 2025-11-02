/**
 * Application layer for the Ingestion module.
 * 
 * This package contains the application services, command/query handlers, and orchestration logic.
 * It implements the CQRS pattern and coordinates between domain and infrastructure layers.
 * 
 * Key principles:
 * - Orchestrates business workflows
 * - Implements CQRS with separate command and query handlers
 * - Contains application services for complex operations
 * - Depends on domain layer interfaces
 * - No direct dependencies on infrastructure implementations
 * 
 * Package structure:
 * - batch/upload: Upload file command and handler
 * - batch/process: Process batch command and handler
 * - batch/query: Batch status queries and handlers
 * - bankinfo: Bank information enrichment services
 * - performance: Performance monitoring application services
 */
package com.bcbs239.regtech.modules.ingestion.application;