package com.bcbs239.regtech.metrics.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class DashboardMetricsKey implements Serializable {

    @Column(name = "bank_id", length = 255, nullable = false)
    private String bankId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    public DashboardMetricsKey() {
    }

    public DashboardMetricsKey(String bankId, LocalDate periodStart) {
        this.bankId = bankId;
        this.periodStart = periodStart;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardMetricsKey that = (DashboardMetricsKey) o;
        return Objects.equals(bankId, that.bankId) && Objects.equals(periodStart, that.periodStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankId, periodStart);
    }
}
