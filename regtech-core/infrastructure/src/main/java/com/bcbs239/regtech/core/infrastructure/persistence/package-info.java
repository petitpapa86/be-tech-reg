/**
 * Persistence Infrastructure Capability
 *
 * This capability provides the infrastructure foundation for data persistence,
 * transaction management, and database configuration across the regtech-core module.
 *
 * Key responsibilities:
 * - JPA configuration and setup
 * - Transaction management configuration
 * - Database connection and entity management
 * - Logging configuration for persistence operations
 * - Data access layer infrastructure
 *
 * Components:
 * - ModularJpaConfiguration: JPA entity manager and transaction configuration
 * - SharedTransactionConfiguration: Cross-module transaction management
 * - LoggingConfiguration: Persistence operation logging and monitoring
 */
package com.bcbs239.regtech.core.infrastructure.persistence;