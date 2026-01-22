package com.bcbs239.regtech.reportgeneration.infrastructure.storage;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class StorageMetadataBuilder {
    private StorageMetadataBuilder() {}

    public static Map<String, String> buildForHtml(
            String batchId,
            String bankId,
            String reportingDate,
            String qualityScore,
            String bankName
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("batch-id", batchId);
        metadata.put("bank-id", bankId);
        metadata.put("reporting-date", reportingDate);
        metadata.put("quality-score", qualityScore == null ? "" : qualityScore);
        metadata.put("generated-at", Instant.now().toString());
        metadata.put("bank-name", bankName == null ? "" : bankName);
        metadata.put("content-type", "text/html");
        return metadata;
    }

    public static Map<String, String> buildForXbrl(
            String batchId,
            String bankId,
            String reportingDate,
            String bankName
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("batch-id", batchId);
        metadata.put("bank-id", bankId);
        metadata.put("reporting-date", reportingDate);
        metadata.put("generated-at", Instant.now().toString());
        metadata.put("bank-name", bankName == null ? "" : bankName);
        metadata.put("content-type", "application/xml");
        return metadata;
    }
}
