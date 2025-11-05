package com.bcbs239.regtech.core.application.shared;

import java.util.List;
import java.util.Optional;

/**
 * Result wrapper for application layer operations.
 * Provides a consistent way to handle success and failure responses.
 */
public record Result<T>(T value, List<ErrorDetail> errors) {

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

    @Override
    public List<ErrorDetail> errors() {
        return errors != null ? errors : List.of();
    }
}