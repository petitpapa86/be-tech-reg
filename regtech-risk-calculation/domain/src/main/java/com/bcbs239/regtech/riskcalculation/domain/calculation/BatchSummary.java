package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.CalculationStatus;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationStartedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationFailedEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for risk calculation batch summary
 * Represents the complete risk calculation results for a batch of exposures
 */
@Getter
public class BatchSummary extends Entity {
    
    private final BatchSummaryId batchSummaryId;
    private final BatchId batchId;
    private final BankId bankId;
    private CalculationStatus status;
    private TotalExposures totalExposures;
    private TotalAmountEur totalAmountEur;
    private GeographicBreakdown geographicBreakdown;
    private SectorBreakdown sectorBreakdown;
    private ConcentrationIndices concentrationIndices;
    private FileStorageUri resultFileUri;
    private ProcessingTimestamps timestamps;
    private String errorMessage;
    
    /**
     * Constructor for creating a new batch summary
     */
    public BatchSummary(BatchId batchId, BankId bankId) {
        this.batchSummaryId = BatchSummaryId.generate();
        this.batchId = Objects.requireNonNull(batchId, "BatchId cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "BankId cannot be null");
        this.status = CalculationStatus.PENDING;
        this.timestamps = ProcessingTimestamps.started();
    }
    
    /**
     * Constructor for reconstituting from persistence
     */
    public BatchSummary(BatchSummaryId batchSummaryId, BatchId batchId, BankId bankId,
                       CalculationStatus status, TotalExposures totalExposures,
                       TotalAmountEur totalAmountEur, GeographicBreakdown geographicBreakdown,
                       SectorBreakdown sectorBreakdown, ConcentrationIndices concentrationIndices,
                       FileStorageUri resultFileUri, ProcessingTimestamps timestamps,
                       String errorMessage) {
        this.batchSummaryId = batchSummaryId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.status = status;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.geographicBreakdown = geographicBreakdown;
        this.sectorBreakdown = sectorBreakdown;
        this.concentrationIndices = concentrationIndices;
        this.resultFileUri = resultFileUri;
        this.timestamps = timestamps;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Start the risk calculation process
     * DDD: Ask the object to do the work
     */
    public Result<Void> startCalculation() {
        if (status != CalculationStatus.PENDING) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot start calculation from status: " + status, "calculation.invalid.transition"));
        }
        
        this.status = CalculationStatus.DOWNLOADING;
        this.timestamps = ProcessingTimestamps.started();
        
        addDomainEvent(new BatchCalculationStartedEvent(this.batchId, this.bankId, Instant.now()));
        
        return Result.success(null);
    }
    
    /**
     * Mark calculation as in progress (calculating)
     */
    public Result<Void> markAsCalculating() {
        if (status != CalculationStatus.DOWNLOADING) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot start calculating from status: " + status, "calculation.invalid.transition"));
        }
        
        this.status = CalculationStatus.CALCULATING;
        return Result.success(null);
    }
    
    /**
     * Complete the risk calculation with results
     * DDD: Ask the object to do the work
     */
    public Result<Void> completeCalculation(TotalExposures totalExposures,
                                          TotalAmountEur totalAmountEur,
                                          GeographicBreakdown geographicBreakdown,
                                          SectorBreakdown sectorBreakdown,
                                          ConcentrationIndices concentrationIndices,
                                          FileStorageUri resultFileUri) {
        
        if (status != CalculationStatus.CALCULATING) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot complete calculation from status: " + status, "calculation.invalid.transition"));
        }
        
        // Validate required data
        Objects.requireNonNull(totalExposures, "Total exposures cannot be null");
        Objects.requireNonNull(totalAmountEur, "Total amount cannot be null");
        Objects.requireNonNull(geographicBreakdown, "Geographic breakdown cannot be null");
        Objects.requireNonNull(sectorBreakdown, "Sector breakdown cannot be null");
        Objects.requireNonNull(concentrationIndices, "Concentration indices cannot be null");
        Objects.requireNonNull(resultFileUri, "Result file URI cannot be null");
        
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.geographicBreakdown = geographicBreakdown;
        this.sectorBreakdown = sectorBreakdown;
        this.concentrationIndices = concentrationIndices;
        this.resultFileUri = resultFileUri;
        this.status = CalculationStatus.COMPLETED;
        this.timestamps = this.timestamps.completed();
        this.errorMessage = null; // Clear any previous error
        
        addDomainEvent(new BatchCalculationCompletedEvent(
            this.batchId, this.bankId, this.resultFileUri, 
            this.totalExposures, this.totalAmountEur, this.concentrationIndices, Instant.now()));
        
        return Result.success(null);
    }
    
    /**
     * Mark calculation as failed with error message
     * DDD: Ask the object to do the work
     */
    public Result<Void> failCalculation(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        
        if (status == CalculationStatus.COMPLETED) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot fail a completed calculation", "calculation.invalid.transition"));
        }
        
        this.status = CalculationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.timestamps = this.timestamps.failed();
        
        addDomainEvent(new BatchCalculationFailedEvent(this.batchId, this.bankId, errorMessage, Instant.now()));
        
        return Result.success(null);
    }
    
    /**
     * Check if calculation is in a terminal state
     */
    public boolean isTerminal() {
        return status == CalculationStatus.COMPLETED || status == CalculationStatus.FAILED;
    }
    
    /**
     * Check if calculation was successful
     */
    public boolean isSuccessful() {
        return status == CalculationStatus.COMPLETED;
    }
    
    /**
     * Check if calculation is in progress
     */
    public boolean isInProgress() {
        return status == CalculationStatus.DOWNLOADING || status == CalculationStatus.CALCULATING;
    }
    
    public BatchSummaryId getId() {
        return batchSummaryId;
    }
}