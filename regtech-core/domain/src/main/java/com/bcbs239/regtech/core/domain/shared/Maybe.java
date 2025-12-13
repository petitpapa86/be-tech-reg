package com.bcbs239.regtech.core.domain.shared;

import java.util.function.Function;
import java.util.function.Supplier;

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

    default <U> Maybe<U> map(Function<? super T, ? extends U> mapper) {
        return isPresent() ? some(mapper.apply(getValue())) : none();
    }

    default <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> mapper) {
        return isPresent() ? mapper.apply(getValue()) : none();
    }

    default T orElseGet(Supplier<? extends T> supplier) {
        return isPresent() ? getValue() : supplier.get();
    }

    default Maybe<T> or(Supplier<Maybe<T>> supplier) {
        return isPresent() ? this : supplier.get();
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

