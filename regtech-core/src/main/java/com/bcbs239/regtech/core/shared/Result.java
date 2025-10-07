package com.bcbs239.regtech.core.shared;

import java.util.Optional;
import java.util.function.Function;

public class Result<T> {

    private final T value;
    private final ErrorDetail error;

    private Result(T value, ErrorDetail error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(ErrorDetail error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<ErrorDetail> getError() {
        return Optional.ofNullable(error);
    }

    public <U> Result<U> map(Function<T, U> mapper) {
        if (isSuccess()) {
            return Result.success(mapper.apply(value));
        } else {
            return Result.failure(error);
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isSuccess()) {
            return mapper.apply(value);
        } else {
            return Result.failure(error);
        }
    }
}