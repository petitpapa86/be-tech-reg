package com.bcbs239.regtech.billing.domain.invoicing;

public enum InvoiceStatus {
    DRAFT,
    PENDING,
    SENT,
    PAID,
    OVERDUE,
    FAILED,
    VOIDED,
    CANCELLED
}