package com.bcbs239.regtech.billing.domain.valueobjects;

/**
 * Status enumeration for Invoice aggregate
 */
public enum InvoiceStatus {
    /**
     * Invoice has been created but not yet sent
     */
    DRAFT,
    
    /**
     * Invoice has been sent and is awaiting payment
     */
    PENDING,
    
    /**
     * Invoice has been paid successfully
     */
    PAID,
    
    /**
     * Invoice payment has failed
     */
    FAILED,
    
    /**
     * Invoice is past due date
     */
    OVERDUE,
    
    /**
     * Invoice has been voided/cancelled
     */
    VOIDED;
    
    /**
     * Check if invoice is awaiting payment
     */
    public boolean isAwaitingPayment() {
        return this == PENDING || this == OVERDUE;
    }
    
    /**
     * Check if invoice is successfully paid
     */
    public boolean isPaid() {
        return this == PAID;
    }
    
    /**
     * Check if invoice has payment issues
     */
    public boolean hasPaymentIssues() {
        return this == FAILED || this == OVERDUE;
    }
    
    /**
     * Check if invoice is finalized (cannot be modified)
     */
    public boolean isFinalized() {
        return this == PAID || this == VOIDED;
    }
    
    /**
     * Check if invoice can be sent
     */
    public boolean canBeSent() {
        return this == DRAFT;
    }
}