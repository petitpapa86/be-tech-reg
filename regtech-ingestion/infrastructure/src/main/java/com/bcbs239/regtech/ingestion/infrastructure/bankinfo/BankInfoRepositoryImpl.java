package com.bcbs239.regtech.ingestion.infrastructure.bankinfo;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.bankinfo.IBankInfoRepository;
import com.bcbs239.regtech.ingestion.infrastructure.bankinfo.persistence.BankInfoEntity;
import com.bcbs239.regtech.ingestion.infrastructure.bankinfo.persistence.BankInfoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of IBankInfoRepository.
 * Manages bank information persistence with caching support.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class BankInfoRepositoryImpl implements IBankInfoRepository {
    
    private static final Logger log = LoggerFactory.getLogger(BankInfoRepositoryImpl.class);
    private final BankInfoJpaRepository jpaRepository;
    
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
            return jpaRepository.findByBankId(bankId.value())
                    .map(BankInfoEntity::toDomain)
                    .filter(BankInfo::isFresh);
        } catch (DataAccessException e) {
            log.error("Error finding fresh bank info by ID: {}", bankId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Result<BankInfo> save(BankInfo bankInfo) {
        try {
            BankInfoEntity entity = BankInfoEntity.fromDomain(bankInfo);
            BankInfoEntity savedEntity = jpaRepository.save(entity);
            BankInfo savedBankInfo = savedEntity.toDomain();
            
            log.debug("Successfully saved bank info: {}", bankInfo.bankId().value());
            return Result.success(savedBankInfo);
            
        } catch (DataAccessException e) {
            log.error("Error saving bank info: {}", bankInfo.bankId().value(), e);
            return Result.failure(ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to save bank information: " + e.getMessage(), "database.save.failed"));
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
            Instant cutoffTime = Instant.now().minusSeconds(hoursAgo * 3600L);
            return jpaRepository.findStaleEntries(cutoffTime)
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
                return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "Bank not found: " + bankId.value(), "bank.not.found"));
            }
            
            jpaRepository.deleteByBankId(bankId.value());
            log.debug("Successfully deleted bank info: {}", bankId.value());
            return Result.success(null);
            
        } catch (DataAccessException e) {
            log.error("Error deleting bank info: {}", bankId.value(), e);
            return Result.failure(ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to delete bank information: " + e.getMessage(), "database.delete.failed"));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBankId(BankId bankId) {
        try {
            return jpaRepository.existsByBankId(bankId.value());
        } catch (DataAccessException e) {
            log.error("Error checking bank existence: {}", bankId.value(), e);
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
            return jpaRepository.findByBankStatus(status).size();
        } catch (DataAccessException e) {
            log.error("Error counting bank info entries by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    public Result<Void> updateLastUpdated(BankId bankId, Instant timestamp) {
        try {
            Optional<BankInfoEntity> entityOpt = jpaRepository.findByBankId(bankId.value());
            
            if (entityOpt.isEmpty()) {
                return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "Bank not found: " + bankId.value(), "bank.not.found"));
            }
            
            BankInfoEntity entity = entityOpt.get();
            entity.setLastUpdated(timestamp);
            jpaRepository.save(entity);
            
            log.debug("Successfully updated lastUpdated timestamp for bank: {}", bankId.value());
            return Result.success(null);
            
        } catch (DataAccessException e) {
            log.error("Error updating lastUpdated for bank: {}", bankId.value(), e);
            return Result.failure(ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to update bank timestamp: " + e.getMessage(), "database.update.failed"));
        }
    }
}
