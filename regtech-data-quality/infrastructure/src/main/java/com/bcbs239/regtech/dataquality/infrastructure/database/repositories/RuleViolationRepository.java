package com.bcbs239.regtech.dataquality.infrastructure.database.repositories;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import com.bcbs239.regtech.dataquality.infrastructure.database.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.domain.rules.ResolutionStatus;
import com.bcbs239.regtech.dataquality.domain.rules.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for RuleViolationEntity operations.
 */
@Repository
public interface RuleViolationRepository extends JpaRepository<RuleViolationEntity, Long> {


}
