package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;

import java.io.InputStream;
import java.util.List;

/**
 * Interface for file storage operations in the risk calculation module.
 * Abstracts storage implementation details (S3 vs local filesystem) from the application layer.
 */
public interface IFileStorageService {
    
    /**
     * Download exposure data from the given URI
     * 
     * @param uri the file storage URI from the ingestion module
     * @return Result containing list of exposure records or error details
     */
    Result<List<ExposureRecord>> downloadExposures(FileStorageUri uri);
    
    /**
     * Upload detailed calculation results to storage
     * 
     * @param batchId the batch identifier
     * @param bankId the bank identifier  
     * @param calculationResults the detailed calculation results as JSON
     * @return Result containing the storage URI or error details
     */
    Result<FileStorageUri> uploadCalculationResults(String batchId, String bankId, String calculationResults);
    
    /**
     * Check if the storage service is available and healthy
     * 
     * @return Result containing true if service is healthy, error details otherwise
     */
    Result<Boolean> checkServiceHealth();
}

/**
 * Represents an exposure record from the downloaded file
 */
class ExposureRecord {
    private final String exposureId;
    private final String clientName;
    private final String originalAmount;
    private final String originalCurrency;
    private final String country;
    private final String sector;
    
    public ExposureRecord(String exposureId, String clientName, String originalAmount, 
                         String originalCurrency, String country, String sector) {
        this.exposureId = exposureId;
        this.clientName = clientName;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.country = country;
        this.sector = sector;
    }
    
    // Getters
    public String getExposureId() { return exposureId; }
    public String getClientName() { return clientName; }
    public String getOriginalAmount() { return originalAmount; }
    public String getOriginalCurrency() { return originalCurrency; }
    public String getCountry() { return country; }
    public String getSector() { return sector; }
}