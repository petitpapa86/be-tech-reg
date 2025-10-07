package com.bcbs239.regtech.billing.application.sagas;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.saga.SagaData;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.time.LocalDate;
import java.util.Currency;

/**
 * Saga data for monthly billing process.
 * Tracks the state of billing cycle execution including usage metrics,
 * charge calculations, and invoice generation.
 */
public class MonthlyBillingSagaData extends SagaData {
    
    // User and billing period identification
    private UserId userId;
    private String billingPeriodId; // Format: "2024-01"
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Usage metrics from ingestion context
    private int totalExposures;
    private int documentsProcessed;
    private long dataVolumeBytes;
    
    // Charge calculations
    private Money subscriptionCharges;
    private Money overageCharges;
    private Money totalCharges;
    
    // Invoice generation
    private InvoiceId generatedInvoiceId;
    private String stripeInvoiceId;
    
    // Billing step progression
    private BillingStep currentStep = BillingStep.GATHER_METRICS;
    private String lastStepError;
    
    /**
     * Enumeration of billing steps for saga progression
     */
    public enum BillingStep {
        GATHER_METRICS,      // Query usage metrics from ingestion context
        CALCULATE_CHARGES,   // Calculate subscription and overage charges
        GENERATE_INVOICE,    // Generate invoice through Stripe integration
        FINALIZE_BILLING     // Complete billing process and publish events
    }
    
    // Constructors
    public MonthlyBillingSagaData() {
        super();
        // Initialize with zero amounts in EUR
        this.subscriptionCharges = Money.zero(Currency.getInstance("EUR"));
        this.overageCharges = Money.zero(Currency.getInstance("EUR"));
        this.totalCharges = Money.zero(Currency.getInstance("EUR"));
    }
    
    /**
     * Factory method to create saga data for a user and billing period
     */
    public static MonthlyBillingSagaData create(UserId userId, BillingPeriod billingPeriod) {
        MonthlyBillingSagaData sagaData = new MonthlyBillingSagaData();
        sagaData.userId = userId;
        sagaData.billingPeriodId = billingPeriod.getPeriodId();
        sagaData.startDate = billingPeriod.startDate();
        sagaData.endDate = billingPeriod.endDate();
        
        // Set correlation ID in format: userId-billingPeriod (e.g., "user-123-2024-01")
        sagaData.setCorrelationId(userId.getValue() + "-" + billingPeriod.getPeriodId());
        
        return sagaData;
    }
    
    // Business logic methods
    
    /**
     * Advance to the next billing step
     */
    public void advanceToNextStep() {
        switch (currentStep) {
            case GATHER_METRICS -> currentStep = BillingStep.CALCULATE_CHARGES;
            case CALCULATE_CHARGES -> currentStep = BillingStep.GENERATE_INVOICE;
            case GENERATE_INVOICE -> currentStep = BillingStep.FINALIZE_BILLING;
            case FINALIZE_BILLING -> {
                // Already at final step
            }
        }
        this.lastStepError = null; // Clear any previous error
    }
    
    /**
     * Mark current step as failed with error message
     */
    public void markStepFailed(String errorMessage) {
        this.lastStepError = errorMessage;
        this.setLastError(errorMessage);
    }
    
    /**
     * Check if saga is at the final step
     */
    public boolean isAtFinalStep() {
        return currentStep == BillingStep.FINALIZE_BILLING;
    }
    
    /**
     * Check if usage metrics have been gathered
     */
    public boolean hasUsageMetrics() {
        return currentStep.ordinal() > BillingStep.GATHER_METRICS.ordinal();
    }
    
    /**
     * Check if charges have been calculated
     */
    public boolean hasCalculatedCharges() {
        return currentStep.ordinal() > BillingStep.CALCULATE_CHARGES.ordinal();
    }
    
    /**
     * Check if invoice has been generated
     */
    public boolean hasGeneratedInvoice() {
        return generatedInvoiceId != null;
    }
    
    /**
     * Get billing period from stored dates
     */
    public BillingPeriod getBillingPeriod() {
        return new BillingPeriod(startDate, endDate);
    }
    
    /**
     * Calculate total charges from subscription and overage
     */
    public void calculateTotalCharges() {
        if (subscriptionCharges != null && overageCharges != null) {
            this.totalCharges = subscriptionCharges.add(overageCharges)
                .getValue().orElse(Money.zero(Currency.getInstance("EUR")));
        }
    }
    
    // Getters and setters
    
    public UserId getUserId() { return userId; }
    public void setUserId(UserId userId) { this.userId = userId; }
    
    public String getBillingPeriodId() { return billingPeriodId; }
    public void setBillingPeriodId(String billingPeriodId) { this.billingPeriodId = billingPeriodId; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public int getTotalExposures() { return totalExposures; }
    public void setTotalExposures(int totalExposures) { this.totalExposures = totalExposures; }
    
    public int getDocumentsProcessed() { return documentsProcessed; }
    public void setDocumentsProcessed(int documentsProcessed) { this.documentsProcessed = documentsProcessed; }
    
    public long getDataVolumeBytes() { return dataVolumeBytes; }
    public void setDataVolumeBytes(long dataVolumeBytes) { this.dataVolumeBytes = dataVolumeBytes; }
    
    public Money getSubscriptionCharges() { return subscriptionCharges; }
    public void setSubscriptionCharges(Money subscriptionCharges) { 
        this.subscriptionCharges = subscriptionCharges;
        calculateTotalCharges();
    }
    
    public Money getOverageCharges() { return overageCharges; }
    public void setOverageCharges(Money overageCharges) { 
        this.overageCharges = overageCharges;
        calculateTotalCharges();
    }
    
    public Money getTotalCharges() { return totalCharges; }
    
    public InvoiceId getGeneratedInvoiceId() { return generatedInvoiceId; }
    public void setGeneratedInvoiceId(InvoiceId generatedInvoiceId) { this.generatedInvoiceId = generatedInvoiceId; }
    
    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public void setStripeInvoiceId(String stripeInvoiceId) { this.stripeInvoiceId = stripeInvoiceId; }
    
    public BillingStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(BillingStep currentStep) { this.currentStep = currentStep; }
    
    public String getLastStepError() { return lastStepError; }
    public void setLastStepError(String lastStepError) { this.lastStepError = lastStepError; }
    
    @Override
    public String toString() {
        return String.format("MonthlyBillingSagaData{userId=%s, billingPeriodId='%s', currentStep=%s, status=%s}",
                userId, billingPeriodId, currentStep, getStatus());
    }
}