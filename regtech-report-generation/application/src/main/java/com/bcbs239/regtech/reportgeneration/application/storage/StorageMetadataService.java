package com.bcbs239.regtech.reportgeneration.application.storage;

import java.util.Map;

/**
 * Port for building storage metadata for generated reports.
 * Implemented in infrastructure to avoid application -> infrastructure dependency.
 */
public interface StorageMetadataService {

    Map<String, String> buildForHtml(String batchId,
                                     String bankId,
                                     String reportingDate,
                                     String overallScore,
                                     String bankName);

    Map<String, String> buildForXbrl(String batchId,
                                     String bankId,
                                     String reportingDate,
                                     String bankName);

    /**
     * Gets the configured storage bucket name.
     * @return the bucket name
     */
    String getStorageBucket();
}
