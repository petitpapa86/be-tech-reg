package com.bcbs239.regtech.core.domain.core;

/**
 * Maybe type for optional values in functional programming
 */
public sealed interface Maybe<T> {

    static <T> Maybe<T> some(T value) {
        return new Some<>(value);
    }

    static <T> Maybe<T> none() {
        return new None<>();
    }

    boolean isPresent();
    boolean isEmpty();
    T getValue();
    
    default T orElse(T defaultValue) {
        return isPresent() ? getValue() : defaultValue;
    }

    record Some<T>(T value) implements Maybe<T> {
        @Override public boolean isPresent() { return true; }
        @Override public boolean isEmpty() { return false; }
        @Override public T getValue() { return value; }
    }

    record None<T>() implements Maybe<T> {
        @Override public boolean isPresent() { return false; }
        @Override public boolean isEmpty() { return true; }
        @Override public T getValue() { throw new IllegalStateException("None has no value"); }
    }
}

