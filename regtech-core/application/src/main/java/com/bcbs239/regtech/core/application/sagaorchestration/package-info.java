/**
 * Saga Orchestration Capability
 *
 * This capability manages distributed transaction orchestration using the saga pattern.
 * It coordinates complex business processes that span multiple services and ensures
 * data consistency across distributed operations.
 *
 * Key responsibilities:
 * - Saga lifecycle management
 * - Distributed transaction coordination
 * - Saga state tracking and persistence
 * - Compensation logic execution
 * - Saga event handling and routing
 *
 * Components:
 * - Saga orchestrators and coordinators
 * - Saga command processors
 * - Saga state management
 * - Compensation action handlers
 * - Saga monitoring and metrics
 *
 * Note: Most saga implementation classes are located in the infrastructure layer.
 * This capability contains application-level saga orchestration logic and coordination.
 */
package com.bcbs239.regtech.core.application.sagaorchestration;

