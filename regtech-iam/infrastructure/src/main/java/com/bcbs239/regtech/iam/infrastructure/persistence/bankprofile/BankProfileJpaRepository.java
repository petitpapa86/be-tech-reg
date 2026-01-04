package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface BankProfileJpaRepository extends JpaRepository<BankProfileJpaEntity, Long> {
    
    /**
     * Find bank profile by bank ID
     */
    Optional<BankProfileJpaEntity> findByBankId(Long bankId);
}
