package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsEntity;
import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SpringDataDashboardMetricsRepository extends JpaRepository<DashboardMetricsEntity, DashboardMetricsKey> {

	Optional<DashboardMetricsEntity> findByKeyBankIdAndKeyPeriodStart(String bankId, LocalDate periodStart);

}
