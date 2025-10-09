package com.bcbs239.regtech.core.events;

/**
 * Marker interface for domain events exchanged between bounded contexts.
 */
public interface DomainEvent {
    String eventType();
}
