package com.bcbs239.regtech.metrics.domain;

import java.util.Objects;

/**
 * Value object representing a Bank identifier. Immutable and non-null.
 */
public final class BankId {
    private final String value;

    private BankId(String value) {
        this.value = value;
    }

    public static BankId of(String value) {
        if (value == null) throw new IllegalArgumentException("bankId cannot be null");
        String v = value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("bankId cannot be empty");
        return new BankId(v);
    }

    public static BankId unknown() {
        return new BankId("UNKNOWN");
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankId bankId = (BankId) o;
        return Objects.equals(value, bankId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
