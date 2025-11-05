package com.bcbs239.regtech.core.domain.events;

/**
 * Marker interface for domain events exchanged between bounded contexts.
 */
public interface DomainEvent {
    String eventType();
}

