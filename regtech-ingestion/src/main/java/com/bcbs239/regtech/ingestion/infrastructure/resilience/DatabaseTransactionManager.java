package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service for managing database transactions with proper rollback and error handling.
 * Provides transactional operations with automatic rollback on failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseTransactionManager {
    
    private final PlatformTransactionManager transactionManager;
    private final IngestionBatchRepository batchRepository;
    
    /**
     * Executes an operation within a transaction with automatic rollback on failure.
     */
    public <T> Result<T> executeInTransaction(String operationName, Supplier<Result<T>> operation) {
        log.debug("Starting transaction for operation: {}", operationName);
        
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionDefinition.setTimeout(30); // 30 seconds timeout
        
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        
        try {
            Result<T> result = operation.get();
            
            if (result.isSuccess()) {
                transactionManager.commit(transactionStatus);
                log.info("Transaction committed successfully for operation: {}", operationName);
                return result;
            } else {
                transactionManager.rollback(transactionStatus);
                log.warn("Transaction rolled back due to operation failure: {}", operationName);
                return result;
            }
            
        } catch (Exception e) {
            try {
                transactionManager.rollback(transactionStatus);
                log.error("Transaction rolled back due to exception in operation {}: {}", operationName, e.getMessage());
            } catch (Exception rollbackException) {
                log.error("Failed to rollback transaction for operation {}: {}", operationName, rollbackException.getMessage());
            }
            
            return Result.failure(ErrorDetail.of("TRANSACTION_FAILED", 
                String.format("Transaction failed for operation %s: %s", operationName, e.getMessage())));
        }
    }
    
    /**
     * Executes a batch processing operation with comprehensive error handling and rollback.
     */
    public Result<Void> executeBatchProcessingTransaction(BatchId batchId, 
                                                         Supplier<Result<Void>> batchOperation) {
        String operationName = "batch-processing-" + batchId.value();
        log.info("Starting batch processing transaction for batch: {}", batchId.value());
        
        return executeInTransaction(operationName, () -> {
            try {
                // Step 1: Load the batch to ensure it exists and is in correct state
                Optional<IngestionBatch> batchOpt = batchRepository.findByBatchId(batchId);
                if (batchOpt.isEmpty()) {
                    return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", 
                        "Batch not found: " + batchId.value()));
                }
                
                IngestionBatch batch = batchOpt.get();
                log.debug("Loaded batch {} with status {}", batchId.value(), batch.getStatus());
                
                // Step 2: Execute the batch operation
                Result<Void> operationResult = batchOperation.get();
                
                if (operationResult.isFailure()) {
                    // Mark batch as failed before rolling back
                    String errorMessage = operationResult.getErrors().stream()
                            .map(ErrorDetail::getMessage)
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("Unknown error");
                    
                    batch.markAsFailed(errorMessage);
                    batchRepository.save(batch);
                    
                    log.error("Batch processing failed for batch {}: {}", batchId.value(), errorMessage);
                    return operationResult;
                }
                
                log.info("Batch processing transaction completed successfully for batch: {}", batchId.value());
                return Result.success(null);
                
            } catch (Exception e) {
                log.error("Unexpected error in batch processing transaction for batch {}: {}", 
                         batchId.value(), e.getMessage());
                
                // Try to mark batch as failed even in case of unexpected error
                try {
                    Optional<IngestionBatch> batchOpt = batchRepository.findByBatchId(batchId);
                    if (batchOpt.isPresent()) {
                        IngestionBatch batch = batchOpt.get();
                        batch.markAsFailed("Unexpected error: " + e.getMessage());
                        batchRepository.save(batch);
                    }
                } catch (Exception saveException) {
                    log.error("Failed to mark batch as failed after unexpected error: {}", saveException.getMessage());
                }
                
                return Result.failure(ErrorDetail.of("BATCH_PROCESSING_ERROR", 
                    "Unexpected error in batch processing: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Executes multiple operations in a single transaction with rollback on any failure.
     */
    public Result<Void> executeMultipleOperations(String operationName, List<Supplier<Result<Void>>> operations) {
        log.debug("Starting multi-operation transaction: {} with {} operations", operationName, operations.size());
        
        return executeInTransaction(operationName, () -> {
            List<ErrorDetail> allErrors = new ArrayList<>();
            
            for (int i = 0; i < operations.size(); i++) {
                try {
                    Result<Void> result = operations.get(i).get();
                    
                    if (result.isFailure()) {
                        log.error("Operation {} failed in multi-operation transaction {}", i + 1, operationName);
                        allErrors.addAll(result.getErrors());
                        break; // Stop on first failure
                    }
                    
                    log.debug("Operation {} completed successfully in transaction {}", i + 1, operationName);
                    
                } catch (Exception e) {
                    log.error("Exception in operation {} of transaction {}: {}", i + 1, operationName, e.getMessage());
                    allErrors.add(ErrorDetail.of("OPERATION_EXCEPTION", 
                        String.format("Exception in operation %d: %s", i + 1, e.getMessage())));
                    break; // Stop on first exception
                }
            }
            
            if (allErrors.isEmpty()) {
                log.info("All {} operations completed successfully in transaction: {}", operations.size(), operationName);
                return Result.success(null);
            } else {
                log.error("Multi-operation transaction {} failed with {} errors", operationName, allErrors.size());
                return Result.failure(allErrors);
            }
        });
    }
    
    /**
     * Executes an operation with a new transaction (REQUIRES_NEW propagation).
     */
    public <T> Result<T> executeInNewTransaction(String operationName, Supplier<Result<T>> operation) {
        log.debug("Starting new transaction for operation: {}", operationName);
        
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionDefinition.setTimeout(30);
        
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        
        try {
            Result<T> result = operation.get();
            
            if (result.isSuccess()) {
                transactionManager.commit(transactionStatus);
                log.info("New transaction committed successfully for operation: {}", operationName);
                return result;
            } else {
                transactionManager.rollback(transactionStatus);
                log.warn("New transaction rolled back due to operation failure: {}", operationName);
                return result;
            }
            
        } catch (Exception e) {
            try {
                transactionManager.rollback(transactionStatus);
                log.error("New transaction rolled back due to exception in operation {}: {}", operationName, e.getMessage());
            } catch (Exception rollbackException) {
                log.error("Failed to rollback new transaction for operation {}: {}", operationName, rollbackException.getMessage());
            }
            
            return Result.failure(ErrorDetail.of("NEW_TRANSACTION_FAILED", 
                String.format("New transaction failed for operation %s: %s", operationName, e.getMessage())));
        }
    }
    
    /**
     * Validates transaction state and provides recovery recommendations.
     */
    public Result<TransactionHealthStatus> checkTransactionHealth() {
        log.debug("Checking transaction health");
        
        try {
            // Test a simple read-only transaction
            DefaultTransactionDefinition readOnlyDef = new DefaultTransactionDefinition();
            readOnlyDef.setReadOnly(true);
            readOnlyDef.setTimeout(5);
            
            TransactionStatus status = transactionManager.getTransaction(readOnlyDef);
            
            try {
                // Perform a simple database operation to test connectivity
                long batchCount = batchRepository.count();
                transactionManager.commit(status);
                
                log.debug("Transaction health check passed, found {} batches", batchCount);
                
                return Result.success(new TransactionHealthStatus(
                    true,
                    "Transaction manager is healthy",
                    batchCount,
                    System.currentTimeMillis()
                ));
                
            } catch (Exception e) {
                transactionManager.rollback(status);
                throw e;
            }
            
        } catch (Exception e) {
            log.error("Transaction health check failed: {}", e.getMessage());
            
            return Result.failure(ErrorDetail.of("TRANSACTION_HEALTH_CHECK_FAILED", 
                "Transaction health check failed: " + e.getMessage()));
        }
    }
    
    /**
     * Provides transaction recovery recommendations based on error analysis.
     */
    public List<String> getTransactionRecoveryRecommendations(Exception transactionException) {
        List<String> recommendations = new ArrayList<>();
        
        String message = transactionException.getMessage();
        if (message == null) {
            message = transactionException.getClass().getSimpleName();
        }
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("timeout") || lowerMessage.contains("deadlock")) {
            recommendations.add("Consider reducing transaction scope or increasing timeout values");
            recommendations.add("Check for long-running operations that might be causing deadlocks");
            recommendations.add("Review database indexes to improve query performance");
        }
        
        if (lowerMessage.contains("connection") || lowerMessage.contains("network")) {
            recommendations.add("Check database connectivity and network stability");
            recommendations.add("Verify database connection pool configuration");
            recommendations.add("Consider implementing connection retry logic");
        }
        
        if (lowerMessage.contains("constraint") || lowerMessage.contains("violation")) {
            recommendations.add("Review data integrity constraints and validation logic");
            recommendations.add("Check for duplicate key violations or foreign key constraints");
            recommendations.add("Validate input data before starting transactions");
        }
        
        if (lowerMessage.contains("rollback") || lowerMessage.contains("abort")) {
            recommendations.add("Review transaction boundaries and error handling");
            recommendations.add("Consider using savepoints for partial rollback scenarios");
            recommendations.add("Implement proper cleanup procedures after rollback");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Review transaction logs for detailed error information");
            recommendations.add("Consider implementing retry logic for transient failures");
            recommendations.add("Verify database health and resource availability");
        }
        
        return recommendations;
    }
    
    /**
     * Transaction health status for monitoring.
     */
    public record TransactionHealthStatus(
        boolean isHealthy,
        String statusMessage,
        long testQueryResult,
        long checkTimestamp
    ) {}
}