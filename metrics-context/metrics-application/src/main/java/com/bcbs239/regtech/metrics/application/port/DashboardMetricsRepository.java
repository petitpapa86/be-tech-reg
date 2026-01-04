package com.bcbs239.regtech.metrics.application.port;

import com.bcbs239.regtech.metrics.domain.DashboardMetrics;
import com.bcbs239.regtech.metrics.domain.BankId;

import java.time.LocalDate;

/**
 * Application port for dashboard metrics persistence.
 * Implementations live in infrastructure.
 */
/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.dashboard.port}.
 */
@Deprecated(forRemoval = true)
public interface DashboardMetricsRepository {

    /**
     * Returns dashboard metrics for a given bank and month.
     * Implementations should create it if missing.
     */
    DashboardMetrics getForMonth(BankId bankId, LocalDate periodStart);

    DashboardMetrics save(DashboardMetrics metrics);
}
