package com.bcbs239.regtech.billing.domain.dunning;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * DunningCase aggregate root - manages the dunning process for overdue invoices.
 * Handles step progression, action tracking, and case resolution for payment collection.
 */
@Getter
@Setter
public class DunningCase {
    
    private DunningCaseId id;
    private InvoiceId invoiceId;
    private BillingAccountId billingAccountId;
    private DunningCaseStatus status;
    private DunningStep currentStep;
    private LocalDate startDate;
    private LocalDate nextActionDate;
    private Instant resolvedAt;
    private String resolutionReason;
    private List<DunningAction> actions;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    
    // Public constructor for JPA entity mapping
    public DunningCase() {
        this.actions = new ArrayList<>();
    }
    
    /**
     * Factory method to create a new DunningCase for an overdue invoice.
     * 
     * @param invoiceId The overdue invoice that triggered this dunning case
     * @param billingAccountId The billing account associated with the invoice
     * @return New DunningCase with IN_PROGRESS status and STEP_1_REMINDER
     */
    public static DunningCase create(InvoiceId invoiceId, BillingAccountId billingAccountId) {
        Objects.requireNonNull(invoiceId, "InvoiceId cannot be null");
        Objects.requireNonNull(billingAccountId, "BillingAccountId cannot be null");
        
        DunningCase dunningCase = new DunningCase();
        dunningCase.id = DunningCaseId.generate();
        dunningCase.invoiceId = invoiceId;
        dunningCase.billingAccountId = billingAccountId;
        dunningCase.status = DunningCaseStatus.IN_PROGRESS;
        dunningCase.currentStep = DunningStep.getFirstStep();
        dunningCase.startDate = LocalDate.now();
        dunningCase.nextActionDate = LocalDate.now().plusDays(dunningCase.currentStep.getDelayFromPrevious().getDays());
        dunningCase.createdAt = Instant.now();
        dunningCase.updatedAt = Instant.now();
        dunningCase.version = 0;
        
        return dunningCase;
    }
    
    /**
     * Execute the current dunning step and record the action.
     * Progresses to the next step if successful, or marks as failed if final step fails.
     * 
     * @param actionType The type of action being executed (e.g., "EMAIL_SENT", "ACCOUNT_SUSPENDED")
     * @param actionDetails Additional details about the action execution
     * @param successful Whether the action was executed successfully
     * @return Result indicating success or failure with error details
     */
    public Result<Void> executeStep(String actionType, String actionDetails, boolean successful) {
        Objects.requireNonNull(actionType, "ActionType cannot be null");
        
        if (this.status != DunningCaseStatus.IN_PROGRESS) {
            return Result.failure(ErrorDetail.of("INVALID_CASE_STATUS", 
                String.format("Cannot execute step for dunning case with status: %s. Expected: %s", 
                    this.status, DunningCaseStatus.IN_PROGRESS), 
                "error.dunning.invalid_case_status"));
        }
        
        // Record the action
        DunningAction action = successful 
            ? DunningAction.successful(this.currentStep, actionType, actionDetails)
            : DunningAction.failed(this.currentStep, actionType, actionDetails);
        
        this.actions.add(action);
        this.updatedAt = Instant.now();
        this.version++;
        
        if (successful) {
            // Progress to next step or complete if this was the final step
            DunningStep nextStep = this.currentStep.getNextStep();
            
            if (nextStep != null) {
                this.currentStep = nextStep;
                this.nextActionDate = LocalDate.now().plusDays(nextStep.getDelayFromPrevious().getDays());
            } else {
                // Final step completed - mark as failed since no payment was received
                this.status = DunningCaseStatus.FAILED;
                this.resolvedAt = Instant.now();
                this.resolutionReason = "All dunning steps completed without payment";
            }
        } else {
            // Action failed - retry logic could be implemented here
            // For now, we'll keep the case in progress but not advance the step
            return Result.failure(ErrorDetail.of("DUNNING_ACTION_FAILED", 
                String.format("Failed to execute dunning action: %s. Details: %s", 
                    actionType, actionDetails), 
                "error.dunning.action_failed"));
        }
        
        return Result.success(null);
    }
    
    /**
     * Resolve the dunning case when payment is received or account is brought current.
     * Transitions from IN_PROGRESS to RESOLVED status.
     * 
     * @param resolutionReason The reason for resolution (e.g., "Payment received", "Account credited")
     * @return Result indicating success or failure with error details
     */
    public Result<Void> resolve(String resolutionReason) {
        Objects.requireNonNull(resolutionReason, "Resolution reason cannot be null");
        
        if (this.status != DunningCaseStatus.IN_PROGRESS) {
            return Result.failure(ErrorDetail.of("INVALID_CASE_STATUS", 
                String.format("Cannot resolve dunning case with status: %s. Expected: %s", 
                    this.status, DunningCaseStatus.IN_PROGRESS), 
                "error.dunning.invalid_case_status"));
        }
        
        this.status = DunningCaseStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolutionReason = resolutionReason;
        this.updatedAt = Instant.now();
        this.version++;
        
        // Record resolution action
        DunningAction resolutionAction = DunningAction.successful(
            this.currentStep, 
            "CASE_RESOLVED", 
            resolutionReason
        );
        this.actions.add(resolutionAction);
        
        return Result.success(null);
    }
    
    /**
     * Cancel the dunning case before completion.
     * Can be used when account is cancelled or manual intervention occurs.
     * 
     * @param cancellationReason The reason for cancellation
     * @return Result indicating success or failure with error details
     */
    public Result<Void> cancel(String cancellationReason) {
        Objects.requireNonNull(cancellationReason, "Cancellation reason cannot be null");
        
        if (this.status != DunningCaseStatus.IN_PROGRESS) {
            return Result.failure(ErrorDetail.of("INVALID_CASE_STATUS", 
                String.format("Cannot cancel dunning case with status: %s. Expected: %s", 
                    this.status, DunningCaseStatus.IN_PROGRESS), 
                "error.dunning.invalid_case_status"));
        }
        
        this.status = DunningCaseStatus.CANCELLED;
        this.resolvedAt = Instant.now();
        this.resolutionReason = cancellationReason;
        this.updatedAt = Instant.now();
        this.version++;
        
        // Record cancellation action
        DunningAction cancellationAction = DunningAction.successful(
            this.currentStep, 
            "CASE_CANCELLED", 
            cancellationReason
        );
        this.actions.add(cancellationAction);
        
        return Result.success(null);
    }
    
    /**
     * Check if the dunning case is ready for the next action.
     * Compares current date with the scheduled next action date.
     * 
     * @return true if the next action should be executed, false otherwise
     */
    public boolean isReadyForNextAction() {
        if (this.status != DunningCaseStatus.IN_PROGRESS) {
            return false;
        }
        
        return LocalDate.now().isAfter(this.nextActionDate) || 
               LocalDate.now().isEqual(this.nextActionDate);
    }
    
    /**
     * Get the most recent action executed for this dunning case.
     * 
     * @return Optional containing the most recent DunningAction, or empty if no actions
     */
    public Optional<DunningAction> getLastAction() {
        if (this.actions.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(this.actions.get(this.actions.size() - 1));
    }
    
    /**
     * Get all actions executed for this dunning case.
     * Returns an immutable view of the actions list.
     * 
     * @return Immutable list of all DunningActions
     */
    public List<DunningAction> getActions() {
        return Collections.unmodifiableList(this.actions);
    }
    
    /**
     * Check if this dunning case has completed all steps without resolution.
     * 
     * @return true if all steps completed and case failed, false otherwise
     */
    public boolean hasExhaustedAllSteps() {
        return this.status == DunningCaseStatus.FAILED;
    }
    
    /**
     * Get the number of days since the dunning case was created.
     * 
     * @return Number of days since creation
     */
    public long getDaysSinceCreation() {
        return java.time.temporal.ChronoUnit.DAYS.between(this.startDate, LocalDate.now());
    }
    
    /**
     * Check if the case is in an active state (can be acted upon).
     * 
     * @return true if case is in progress, false if resolved/cancelled/failed
     */
    public boolean isActive() {
        return this.status == DunningCaseStatus.IN_PROGRESS;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DunningCase that = (DunningCase) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("DunningCase{id=%s, invoiceId=%s, status=%s, currentStep=%s, version=%d}", 
            id, invoiceId, status, currentStep, version);
    }
}

