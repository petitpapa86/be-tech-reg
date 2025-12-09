package com.bcbs239.regtech.core.domain.shared;

import com.bcbs239.regtech.core.domain.events.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for domain entities that can raise domain events.
 */
public abstract class Entity {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Get the domain events raised by this entity.
     */
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    /**
     * Clear the domain events after they have been processed.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Add a domain event to be raised.
     */
    protected void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}

