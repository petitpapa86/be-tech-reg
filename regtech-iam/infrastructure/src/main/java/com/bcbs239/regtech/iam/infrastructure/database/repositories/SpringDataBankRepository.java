package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.BankEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for bank entities.
 */
@Repository
public interface SpringDataBankRepository extends JpaRepository<BankEntity, String> {
    
    /**
     * Find a bank by its name.
     * 
     * @param name the bank name
     * @return optional bank entity
     */
    @Query("SELECT b FROM BankEntity b WHERE b.name = :name")
    Optional<BankEntity> findByName(@Param("name") String name);
    
    /**
     * Find banks by country code.
     * 
     * @param countryCode the country code
     * @return list of bank entities
     */
    @Query("SELECT b FROM BankEntity b WHERE b.countryCode = :countryCode")
    java.util.List<BankEntity> findByCountryCode(@Param("countryCode") String countryCode);
}
