package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

/**
 * Batch Context Value Object
 * 
 * Encapsulates batch and bank identifiers that always travel together.
 * This ensures consistency and reduces parameter passing.
 */
public record BatchContext(
    BatchId batchId,
    BankId bankId
) {
    public BatchContext {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
    }
    
    public static BatchContext of(String batchId, String bankId) {
        return new BatchContext(
            BatchId.of(batchId),
            BankId.of(bankId)
        );
    }
    
    public static BatchContext of(BatchId batchId, BankId bankId) {
        return new BatchContext(batchId, bankId);
    }
}
