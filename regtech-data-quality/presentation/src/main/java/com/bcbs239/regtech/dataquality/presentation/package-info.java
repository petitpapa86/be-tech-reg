/**
 * Presentation layer for the data quality module.
 * 
 * This package contains the web layer components for the data quality module:
 * - Controllers: Functional endpoints for quality reports and health monitoring
 * - Exception handlers: Module-specific error handling and response formatting
 * - Configuration: Web routing and endpoint registration
 * - Constants: API tags and common values
 * 
 * The presentation layer follows functional programming patterns using RouterFunction
 * and provides RESTful APIs for:
 * - Quality report retrieval by batch ID
 * - Quality trends analysis over time periods
 * - Health checks for database, S3, and validation engine
 * - Performance metrics and monitoring
 * 
 * All endpoints implement proper JWT authentication and authorization using
 * the existing security infrastructure.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
 */
package com.bcbs239.regtech.dataquality.presentation;

