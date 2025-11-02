package com.bcbs239.regtech.ingestion.domain.specification;

import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;

/**
 * Collection of specifications for IngestionBatch business rules.
 */
public class IngestionBatchSpecifications {
    
    /**
     * Specification that checks if a batch can be processed.
     * A batch can be processed if it's in UPLOADED status and has valid file metadata.
     */
    public static Specification<IngestionBatch> canBeProcessed() {
        return new CanBeProcessedSpecification();
    }
    
    /**
     * Specification that checks if a batch must be parsed.
     * A batch must be parsed if it's in UPLOADED status.
     */
    public static Specification<IngestionBatch> mustBeParsed() {
        return new MustBeParsedSpecification();
    }
    
    /**
     * Specification that checks if a batch can be stored.
     * A batch can be stored if it's validated and has bank information.
     */
    public static Specification<IngestionBatch> canBeStored() {
        return new CanBeStoredSpecification();
    }
    
    /**
     * Specification that checks if a batch must not be failed.
     * A batch must not be in FAILED status.
     */
    public static Specification<IngestionBatch> mustNotBeFailed() {
        return new MustNotBeFailedSpecification();
    }
    
    /**
     * Specification that checks if a batch has valid bank information.
     */
    public static Specification<IngestionBatch> hasBankInfo() {
        return new HasBankInfoSpecification();
    }
    
    /**
     * Specification that checks if a batch has active bank status.
     */
    public static Specification<IngestionBatch> hasActiveBankStatus() {
        return new HasActiveBankStatusSpecification();
    }
    
    // Implementation classes
    
    private static class CanBeProcessedSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getStatus() == BatchStatus.UPLOADED 
                && batch.getFileMetadata().isValid();
        }
    }
    
    private static class MustBeParsedSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getStatus() == BatchStatus.UPLOADED;
        }
    }
    
    private static class CanBeStoredSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getStatus() == BatchStatus.VALIDATED 
                && batch.getBankInfo() != null
                && batch.getBankInfo().isActive();
        }
    }
    
    private static class MustNotBeFailedSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getStatus() != BatchStatus.FAILED;
        }
    }
    
    private static class HasBankInfoSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getBankInfo() != null;
        }
    }
    
    private static class HasActiveBankStatusSpecification implements Specification<IngestionBatch> {
        @Override
        public boolean isSatisfiedBy(IngestionBatch batch) {
            return batch.getBankInfo() != null 
                && batch.getBankInfo().isActive();
        }
    }
}