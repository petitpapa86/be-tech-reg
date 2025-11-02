package com.bcbs239.regtech.ingestion.infrastructure.persistence;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import com.bcbs239.regtech.ingestion.domain.repository.BankInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of BankInfoRepository.
 * Handles caching and persistence of bank information.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BankInfoRepositoryImpl implements BankInfoRepository {
    
    private final BankInfoJpaRepository jpaRepository;
    
    // 24 hours in seconds
    private static final long FRESHNESS_THRESHOLD_SECONDS = 24 * 60 * 60;
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BankInfo> findByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankId(bankId.value())
                    .map(BankInfoEntity::toDomain);
        } catch (DataAccessException e) {
            log.error("Error finding bank info by ID: {}", bankId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BankInfo> findFreshBankInfo(BankId bankId) {
        try {
            Instant freshnessCutoff = Instant.now().minusSeconds(FRESHNESS_THRESHOLD_SECONDS);
            return jpaRepository.findFreshBankInfo(bankId.value(), freshnessCutoff)
                    .map(BankInfoEntity::toDomain);
        } catch (DataAccessException e) {
            log.error("Error finding fresh bank info by ID: {}", bankId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Result<BankInfo> save(BankInfo bankInfo) {
        try {
            // Check if bank info already exists
            Optional<BankInfoEntity> existingEntity = jpaRepository.findByBankId(bankInfo.bankId().value());
            
            BankInfoEntity entity;
            if (existingEntity.isPresent()) {
                // Update existing entity
                entity = existingEntity.get();
                entity.updateWith(bankInfo);
            } else {
                // Create new entity
                entity = BankInfoEntity.fromDomain(bankInfo);
            }
            
            BankInfoEntity savedEntity = jpaRepository.save(entity);
            BankInfo savedBankInfo = savedEntity.toDomain();
            
            log.debug("Successfully saved bank info: {}", bankInfo.bankId().value());
            return Result.success(savedBankInfo);
            
        } catch (DataAccessException e) {
            log.error("Error saving bank info: {}", bankInfo.bankId().value(), e);
            return Result.failure(new ErrorDetail("DATABASE_ERROR", 
                "Failed to save bank information: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BankInfo> findAll() {
        try {
            return jpaRepository.findAll()
                    .stream()
                    .map(BankInfoEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding all bank info", e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BankInfo> findByStatus(BankInfo.BankStatus status) {
        try {
            return jpaRepository.findByBankStatus(status)
                    .stream()
                    .map(BankInfoEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding bank info by status: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BankInfo> findStaleEntries(int hoursAgo) {
        try {
            Instant staleCutoff = Instant.now().minusSeconds(hoursAgo * 60L * 60L);
            return jpaRepository.findStaleEntries(staleCutoff)
                    .stream()
                    .map(BankInfoEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding stale bank info entries", e);
            return List.of();
        }
    }
    
    @Override
    public Result<Void> delete(BankId bankId) {
        try {
            if (!jpaRepository.existsByBankId(bankId.value())) {
                return Result.failure(new ErrorDetail("BANK_INFO_NOT_FOUND", 
                    "Bank info not found: " + bankId.value()));
            }
            
            jpaRepository.deleteByBankId(bankId.value());
            log.debug("Successfully deleted bank info: {}", bankId.value());
            return Result.success(null);
            
        } catch (DataAccessException e) {
            log.error("Error deleting bank info: {}", bankId.value(), e);
            return Result.failure(new ErrorDetail("DATABASE_ERROR", 
                "Failed to delete bank information: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBankId(BankId bankId) {
        try {
            return jpaRepository.existsByBankId(bankId.value());
        } catch (DataAccessException e) {
            log.error("Error checking bank info existence: {}", bankId.value(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        try {
            return jpaRepository.count();
        } catch (DataAccessException e) {
            log.error("Error counting bank info entries", e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByStatus(BankInfo.BankStatus status) {
        try {
            return jpaRepository.countByBankStatus(status);
        } catch (DataAccessException e) {
            log.error("Error counting bank info by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    public Result<Void> updateLastUpdated(BankId bankId, Instant timestamp) {
        try {
            int updatedRows = jpaRepository.updateLastUpdated(bankId.value(), timestamp);
            
            if (updatedRows == 0) {
                return Result.failure(new ErrorDetail("BANK_INFO_NOT_FOUND", 
                    "Bank info not found for update: " + bankId.value()));
            }
            
            log.debug("Successfully updated last updated timestamp for bank: {}", bankId.value());
            return Result.success(null);
            
        } catch (DataAccessException e) {
            log.error("Error updating last updated timestamp for bank: {}", bankId.value(), e);
            return Result.failure(new ErrorDetail("DATABASE_ERROR", 
                "Failed to update bank information timestamp: " + e.getMessage()));
        }
    }
}