package com.bcbs239.regtech.ingestion.infrastructure.persistence;

import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for BankInfoEntity.
 */
@Repository
public interface BankInfoJpaRepository extends JpaRepository<BankInfoEntity, String> {
    
    /**
     * Find bank information by bank ID.
     */
    Optional<BankInfoEntity> findByBankId(String bankId);
    
    /**
     * Find fresh bank information (updated within the last 24 hours).
     */
    @Query("SELECT b FROM BankInfoEntity b WHERE " +
           "b.bankId = :bankId AND b.lastUpdated > :freshnessCutoff")
    Optional<BankInfoEntity> findFreshBankInfo(@Param("bankId") String bankId, 
                                               @Param("freshnessCutoff") Instant freshnessCutoff);
    
    /**
     * Find bank information by status.
     */
    List<BankInfoEntity> findByBankStatus(BankInfo.BankStatus status);
    
    /**
     * Find stale bank information entries (older than specified cutoff).
     */
    @Query("SELECT b FROM BankInfoEntity b WHERE b.lastUpdated < :staleCutoff")
    List<BankInfoEntity> findStaleEntries(@Param("staleCutoff") Instant staleCutoff);
    
    /**
     * Check if bank information exists by bank ID.
     */
    boolean existsByBankId(String bankId);
    
    /**
     * Delete bank information by bank ID.
     */
    void deleteByBankId(String bankId);
    
    /**
     * Count entries by status.
     */
    long countByBankStatus(BankInfo.BankStatus status);
    
    /**
     * Update last updated timestamp for a bank.
     */
    @Modifying
    @Query("UPDATE BankInfoEntity b SET b.lastUpdated = :timestamp WHERE b.bankId = :bankId")
    int updateLastUpdated(@Param("bankId") String bankId, @Param("timestamp") Instant timestamp);
    
    /**
     * Find all active banks.
     */
    @Query("SELECT b FROM BankInfoEntity b WHERE b.bankStatus = 'ACTIVE' ORDER BY b.bankName")
    List<BankInfoEntity> findAllActiveBanks();
    
    /**
     * Find banks that need refresh (older than specified hours).
     */
    @Query("SELECT b FROM BankInfoEntity b WHERE " +
           "b.lastUpdated < :refreshCutoff " +
           "ORDER BY b.lastUpdated ASC")
    List<BankInfoEntity> findBanksNeedingRefresh(@Param("refreshCutoff") Instant refreshCutoff);
}