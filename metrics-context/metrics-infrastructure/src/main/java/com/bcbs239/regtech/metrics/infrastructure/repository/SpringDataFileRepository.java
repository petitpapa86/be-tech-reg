package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.infrastructure.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataFileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByBankId(String bankId);
    List<FileEntity> findByBankIdAndDateBetween(String bankId, String startDate, String endDate);
}
