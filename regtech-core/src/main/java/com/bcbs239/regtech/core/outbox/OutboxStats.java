package com.bcbs239.regtech.core.outbox;

/**
 * Shared outbox statistics record used across the outbox packages.
 */
public record OutboxStats(long pending, long processing, long processed, long failed, long deadLetter) {}
