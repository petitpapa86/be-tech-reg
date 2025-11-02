package com.bcbs239.regtech.core.shared;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Result<T> {

    private final T value;
    private final List<ErrorDetail> errors;

    private Result(T value, List<ErrorDetail> errors) {
        this.value = value;
        this.errors = errors;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static Result<Void> success() {
        return new Result<>(null, null);
    }

    public static <T> Result<T> failure(ErrorDetail error) {
        return new Result<>(null, List.of(error));
    }

    public static <T> Result<T> failure(List<ErrorDetail> errors) {
        return new Result<>(null, errors);
    }

    public static <T> Result<T> failure(String errorCode, String message) {
        return failure(ErrorDetail.of(errorCode, message));
    }

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    public boolean isFailure() {
        return errors != null && !errors.isEmpty();
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<ErrorDetail> getError() {
        return errors != null && !errors.isEmpty() ? Optional.of(errors.get(0)) : Optional.empty();
    }

    public List<ErrorDetail> getErrors() {
        return errors != null ? errors : List.of();
    }

    public <U> Result<U> map(Function<T, U> mapper) {
        if (isSuccess()) {
            return Result.success(mapper.apply(value));
        } else {
            return Result.failure(errors);
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isSuccess()) {
            return mapper.apply(value);
        } else {
            return Result.failure(errors);
        }
    }

    /**
     * Returns the value if successful, otherwise throws an exception
     */
    public T getValueOrThrow() {
        if (isSuccess()) {
            return value;
        }
        throw new IllegalStateException("Cannot get value from failed result: " + 
            (errors != null && !errors.isEmpty() ? errors.get(0).getMessage() : "Unknown error"));
    }

    /**
     * Returns the value if successful, otherwise returns the default value
     */
    public T getValueOrDefault(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    /**
     * Convert Result to Optional, discarding error information
     */
    public Optional<T> toOptional() {
        return getValue();
    }
}