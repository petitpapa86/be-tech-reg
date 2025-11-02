/**
 * Presentation layer for the Ingestion module.
 * 
 * This package contains the REST controllers, DTOs, and web-related configurations.
 * It provides the external API interface for the ingestion functionality.
 * 
 * Key principles:
 * - Provides REST API endpoints
 * - Contains request/response DTOs
 * - Handles HTTP-specific concerns (validation, serialization)
 * - Delegates business logic to application layer
 * - Contains web security and configuration
 * 
 * Package structure:
 * - batch: Batch-related controllers and DTOs
 * - performance: Performance monitoring endpoints
 * - health: Health check endpoints
 * - config: Web configuration and module setup
 */
package com.bcbs239.regtech.modules.ingestion.presentation;