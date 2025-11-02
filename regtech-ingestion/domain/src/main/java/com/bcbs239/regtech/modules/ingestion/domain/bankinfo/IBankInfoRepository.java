package com.bcbs239.regtech.modules.ingestion.domain.bankinfo;

import com.bcbs239.regtech.core.shared.Result;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BankInfo caching.
 * Manages cached bank information with freshness validation.
 */
public interface IBankInfoRepository {
    
    /**
     * Find bank information by bank ID.
     */
    Optional<BankInfo> findByBankId(BankId bankId);
    
    /**
     * Find fresh bank information (less than 24 hours old).
     */
    Optional<BankInfo> findFreshBankInfo(BankId bankId);
    
    /**
     * Save or update bank information.
     */
    Result<BankInfo> save(BankInfo bankInfo);
    
    /**
     * Find all bank information entries.
     */
    List<BankInfo> findAll();
    
    /**
     * Find bank information by status.
     */
    List<BankInfo> findByStatus(BankInfo.BankStatus status);
    
    /**
     * Find stale bank information (older than specified hours).
     */
    List<BankInfo> findStaleEntries(int hoursAgo);
    
    /**
     * Delete bank information (for cleanup).
     */
    Result<Void> delete(BankId bankId);
    
    /**
     * Check if bank information exists.
     */
    boolean existsByBankId(BankId bankId);
    
    /**
     * Count total cached bank entries.
     */
    long count();
    
    /**
     * Count entries by status.
     */
    long countByStatus(BankInfo.BankStatus status);
    
    /**
     * Update last updated timestamp for a bank.
     */
    Result<Void> updateLastUpdated(BankId bankId, Instant timestamp);
}