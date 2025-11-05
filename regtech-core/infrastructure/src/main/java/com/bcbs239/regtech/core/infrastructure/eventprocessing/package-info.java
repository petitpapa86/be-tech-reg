/**
 * Event Processing Infrastructure Capability
 *
 * This capability provides the infrastructure foundation for reliable event processing
 * using inbox/outbox patterns and event-driven communication across the regtech-core module.
 *
 * Key responsibilities:
 * - Inbox/outbox message processing and persistence
 * - Event bus implementations for cross-module communication
 * - Integration event consumption and deserialization
 * - Reliable event delivery and processing
 *
 * Components:
 * - InboxMessageEntity, InboxMessageRepository: Inbox message persistence
 * - OutboxMessageEntity, OutboxMessageRepository, OutboxMessageStatus: Outbox message persistence
 * - InboxMessageConsumer, InboxMessageOperations: Inbox processing logic
 * - OutboxEventBus, CrossModuleEventBus: Event bus implementations
 * - IntegrationEventConsumer, IntegrationEventDeserializer: External event handling
 * - EventDispatcher: Event routing and dispatching
 */
package com.bcbs239.regtech.core.infrastructure.eventprocessing;

