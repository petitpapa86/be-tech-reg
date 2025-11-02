package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;

import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.modules.ingestion.domain.batch.IngestionBatch;

/**
 * Adapter interface to reuse the shared Specification pattern from core.
 * This keeps domain-level specifications typed to IngestionBatch but leverages
 * the existing composition implementations (AndSpecification/OrSpecification/NotSpecification).
 */
public interface BatchSpecification extends Specification<IngestionBatch> {
    // no extra methods; uses Specification<T> defaults
}
