package com.bcbs239.regtech.core.domain.events;

public interface DomainEventBus {
    void publishAsReplay(DomainEvent event);
}
