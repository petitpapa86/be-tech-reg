package com.bcbs239.regtech.riskcalculation.infrastructure.persistence.parameters;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface RiskParametersJpaRepository extends JpaRepository<RiskParametersJpaEntity, String> {
    Optional<RiskParametersJpaEntity> findByBankId(@NonNull String bankId);
}
