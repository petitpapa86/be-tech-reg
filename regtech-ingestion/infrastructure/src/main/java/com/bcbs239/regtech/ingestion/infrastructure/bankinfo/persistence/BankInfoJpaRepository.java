package com.bcbs239.regtech.ingestion.infrastructure.bankinfo.persistence;

import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo.BankStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for BankInfoEntity.
 */
public interface BankInfoJpaRepository extends JpaRepository<BankInfoEntity, String> {
    
    /**
     * Find bank information by bank ID.
     */
    Optional<BankInfoEntity> findByBankId(String bankId);
    
    /**
     * Find bank information by status.
     */
    List<BankInfoEntity> findByBankStatus(BankStatus status);
    
    /**
     * Find stale bank information entries (older than specified timestamp).
     */
    @Query("SELECT b FROM BankInfoEntity b WHERE b.lastUpdated < :cutoffTime")
    List<BankInfoEntity> findStaleEntries(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Delete bank information by bank ID.
     */
    void deleteByBankId(String bankId);
    
    /**
     * Check if bank exists.
     */
    boolean existsByBankId(String bankId);
}
