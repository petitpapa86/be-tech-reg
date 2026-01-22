package com.bcbs239.regtech.reportgeneration.infrastructure.storage;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.bcbs239.regtech.reportgeneration.application.storage.StorageMetadataService;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;

import lombok.RequiredArgsConstructor;

/**
 * Infrastructure implementation delegating to existing StorageMetadataBuilder utility.
 */
@Component
@RequiredArgsConstructor
public class StorageMetadataServiceImpl implements StorageMetadataService {

    private final ReportGenerationProperties properties;

    @Override
    public Map<String, String> buildForHtml(String batchId, String bankId, String reportingDate, String overallScore, String bankName) {
        return StorageMetadataBuilder.buildForHtml(batchId, bankId, reportingDate, overallScore, bankName);
    }

    @Override
    public Map<String, String> buildForXbrl(String batchId, String bankId, String reportingDate, String bankName) {
        return StorageMetadataBuilder.buildForXbrl(batchId, bankId, reportingDate, bankName);
    }

    @Override
    public String getStorageBucket() {
        return properties.getS3().getBucket();
    }
}
