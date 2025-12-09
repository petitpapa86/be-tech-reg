/**
 * Events Capability
 *
 * This capability manages the domain event system and integration events
 * for the regtech-core module. It provides the foundation for event-driven
 * architecture and cross-bounded context communication.
 *
 * Key responsibilities:
 * - Domain event definition and handling
 * - Integration event publishing and consumption
 * - Event bus abstractions and implementations
 * - Event-driven communication patterns
 *
 * Components:
 * - BaseEvent: Foundation for all domain events
 * - DomainEvent: Core domain event interface
 * - DomainEventHandler: Event handler abstractions
 * - EventBus: Domain event bus interface
 * - IntegrationEvent: Cross-bounded context events
 * - IIntegrationEventBus: Integration event bus interface
 * - IIntegrationEventHandler: Integration event handler interface
 * - Specific domain events: BillingAccountStatusChangedEvent, PaymentVerifiedEvent,
 *   SubscriptionCancelledEvent, UserRegisteredEvent, UserRegisteredIntegrationEvent
 */
package com.bcbs239.regtech.core.domain.events;

