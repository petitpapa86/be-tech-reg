package com.bcbs239.regtech.core.inbox;

/**
 * Statistics for inbox processing
 */
public record InboxStats(long pending, long processing, long processed, long failed, long deadLetter) {}