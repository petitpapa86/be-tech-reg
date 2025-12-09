package com.bcbs239.regtech.reportgeneration.domain.storage;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import org.w3c.dom.Document;

import java.util.Map;

/**
 * Domain service interface for report storage operations.
 * 
 * Abstracts the storage mechanism (S3, local filesystem, etc.) from the
 * application layer. Implementations handle uploading reports with encryption,
 * metadata, and presigned URL generation.
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
public interface IReportStorageService {
    
    /**
     * Upload HTML report to storage with encryption and metadata.
     * 
     * @param htmlContent The HTML content to upload
     * @param fileName The file name (e.g., Comprehensive_Risk_Analysis_12345_2024-11-19.html)
     * @param metadata Additional metadata tags
     * @return UploadResult containing S3 URI, file size, and presigned URL
     */
    UploadResult uploadHtmlReport(String htmlContent, String fileName, Map<String, String> metadata);
    
    /**
     * Upload XBRL report to storage with encryption and metadata.
     * 
     * @param xbrlDocument The XBRL XML document to upload
     * @param fileName The file name (e.g., Large_Exposures_12345_2024-11-19.xml)
     * @param metadata Additional metadata tags
     * @return UploadResult containing S3 URI, file size, and presigned URL
     */
    UploadResult uploadXbrlReport(Document xbrlDocument, String fileName, Map<String, String> metadata);
    
    /**
     * Fetch calculation data from storage by batch ID.
     * 
     * @param batchId The batch identifier
     * @return JSON content as string
     */
    String fetchCalculationData(String batchId);
    
    /**
     * Fetch quality data from storage by batch ID.
     * 
     * @param batchId The batch identifier
     * @return JSON content as string
     */
    String fetchQualityData(String batchId);
    
    /**
     * Result object containing upload information.
     */
    record UploadResult(S3Uri s3Uri, FileSize fileSize, PresignedUrl presignedUrl) {}
}
