package com.bcbs239.regtech.dataquality.application.filemanagement;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import java.time.Instant;

/**
 * Query to list quality report files with filters.
 */
public record ListFilesQuery(
    BankId bankId,
    String status,
    Instant dateFrom,
    String format,
    String searchQuery,
    int page,
    int size,
    String sortBy,
    String sortOrder
) {
    public ListFilesQuery {
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
    }
}
