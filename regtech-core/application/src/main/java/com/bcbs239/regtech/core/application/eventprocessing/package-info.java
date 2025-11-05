/**
 * Event Processing Capability
 *
 * This capability handles the reliable processing of domain events using the inbox/outbox pattern.
 * It provides mechanisms for publishing events, processing incoming events, and ensuring
 * reliable event delivery across distributed systems.
 *
 * Key responsibilities:
 * - Outbox event publishing and processing
 * - Inbox event consumption and processing
 * - Event message state management
 * - Reliable event delivery guarantees
 * - Event processing configuration
 *
 * Components:
 * - Event publishers for outbox and inbox patterns
 * - Event processors for reliable message handling
 * - Configuration classes for event processing setup
 * - DTOs and options for event message handling
 */
package com.bcbs239.regtech.core.application.eventprocessing;

