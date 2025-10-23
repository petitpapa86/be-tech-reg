package com.bcbs239.regtech.core.saga;

import org.springframework.context.ApplicationEvent;

import java.util.function.Supplier;

public class GenericEvent<T> extends ApplicationEvent {
    private final T payload;
    private final Supplier<String> typeSupplier;

    public GenericEvent(T payload, Supplier<String> typeSupplier) {
        super(payload);
        this.payload = payload;
        this.typeSupplier = typeSupplier;
    }

    public T getPayload() {
        return payload;
    }

    public String getType() {
        return typeSupplier.get();
    }
}