/**
 * Saga Infrastructure Capability
 *
 * This capability provides the infrastructure foundation for distributed transaction
 * orchestration using the saga pattern. It manages saga lifecycle, persistence,
 * and coordination across multiple services and bounded contexts.
 *
 * Key responsibilities:
 * - Saga persistence and state management
 * - Distributed transaction coordination
 * - Saga lifecycle tracking and monitoring
 * - Compensation logic execution
 * - Saga command processing and routing
 *
 * Components:
 * - Saga, AbstractSaga: Base saga implementations
 * - SagaEntity, JpaSagaRepository: Saga persistence
 * - SagaManager: Saga orchestration and management
 * - SagaCommand, RetrySagaCommand, RequestNewPaymentMethodCommand: Saga commands
 * - SagaConfiguration: Saga infrastructure configuration
 * - Saga events: SagaStartedEvent, SagaCompletedEvent, SagaFailedEvent
 * - Saga exceptions: SagaCreationException, SagaNotFoundException
 */
package com.bcbs239.regtech.core.infrastructure.saga;