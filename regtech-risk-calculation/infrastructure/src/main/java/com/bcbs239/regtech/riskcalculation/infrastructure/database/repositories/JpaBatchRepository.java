package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA implementation of BatchRepository.
 * Adapts Spring Data JPA repository to domain repository interface.
 */
@Repository
@RequiredArgsConstructor
public class JpaBatchRepository implements BatchRepository {
    
    private final SpringDataBatchRepository springDataRepository;
    
    @Override
    @Transactional
    public void createBatch(String batchId, BankInfo bankInfo, LocalDate reportDate, 
                           int totalExposures, Instant ingestedAt) {
        BatchEntity entity = BatchEntity.builder()
            .batchId(batchId)
            .bankName(bankInfo.bankName())
            .abiCode(bankInfo.abiCode())
            .leiCode(bankInfo.leiCode())
            .reportDate(reportDate)
            .totalExposures(totalExposures)
            .status("PROCESSING")
            .ingestedAt(ingestedAt)
            .build();
        
        springDataRepository.save(entity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean exists(String batchId) {
        return springDataRepository.existsById(batchId);
    }
    
    @Override
    @Transactional
    public void updateStatus(String batchId, String status) {
        springDataRepository.findById(batchId).ifPresent(entity -> {
            entity.setStatus(status);
            springDataRepository.save(entity);
        });
    }
    
    @Override
    @Transactional
    public void markAsProcessed(String batchId, Instant processedAt) {
        springDataRepository.findById(batchId).ifPresent(entity -> {
            entity.setProcessedAt(processedAt);
            entity.setStatus("COMPLETED");
            springDataRepository.save(entity);
        });
    }
}
