package com.bcbs239.regtech.core.domain.eventprocessing;

/**
 * Status of event processing failure
 */
public enum EventProcessingStatus {
    PENDING,    // Waiting for retry
    PROCESSING, // Currently being processed
    SUCCEEDED,  // Successfully processed
    FAILED      // Permanently failed after max retries
}