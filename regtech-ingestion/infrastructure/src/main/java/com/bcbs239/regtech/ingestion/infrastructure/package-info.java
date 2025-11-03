/**
 * Infrastructure layer for the Ingestion module.
 * 
 * This package contains the technical implementations of domain interfaces and external service integrations.
 * It handles persistence, external APIs, messaging, and other infrastructure concerns.
 * 
 * Key principles:
 * - Implements domain repository interfaces
 * - Handles external service integrations (S3, Bank Registry)
 * - Contains JPA entities and database mappings
 * - Provides technical services (monitoring, performance, security)
 * - Depends on domain and application layers
 * 
 * Package structure:
 * - batch: IngestionBatch repository implementations and persistence
 * - bankinfo: BankInfo repository implementations and caching
 * - performance: Performance monitoring and optimization services
 * - storage: S3 storage service implementations
 * - events: Event publishing and outbox pattern implementations
 * - configuration: Spring configuration and dependency injection
 */
package com.bcbs239.regtech.ingestion.infrastructure;