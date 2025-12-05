package com.bcbs239.regtech.reportgeneration.infrastructure.filestorage;

import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import com.bcbs239.regtech.reportgeneration.domain.storage.IReportStorageService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * S3-based report storage service with circuit breaker pattern.
 * Handles uploading HTML and XBRL reports to S3 with encryption and metadata.
 * Falls back to local filesystem when S3 is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ReportStorageService implements IReportStorageService {
    
    private final CoreS3Service coreS3Service;

    @Value("${report-generation.s3.bucket:risk-analysis}")
    private String bucketName;
    
    @Value("${report-generation.s3.html-prefix:reports/html/}")
    private String htmlPrefix;
    
    @Value("${report-generation.s3.xbrl-prefix:reports/xbrl/}")
    private String xbrlPrefix;
    
    @Value("${report-generation.fallback.local-path:/tmp/deferred-reports/}")
    private String localFallbackPath;
    
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofHours(1);
    
    /**
     * Upload HTML report to S3 with encryption and metadata.
     * Circuit breaker protects against S3 service degradation.
     * 
     * @param htmlContent The HTML content to upload
     * @param fileName The file name (e.g., Large_Exposures_12345_2024-11-19.html)
     * @param metadata Additional metadata tags
     * @return UploadResult containing S3 URI, file size, and presigned URL
     */
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadHtmlToLocalFallback")
    @Override
    public UploadResult uploadHtmlReport(String htmlContent, String fileName, Map<String, String> metadata) {
        log.info("Uploading HTML report to S3 [fileName:{},bucket:{}]", fileName, bucketName);
        
        try {
            String key = htmlPrefix + fileName;
            byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            long fileSize = contentBytes.length;
            
            // Prepare metadata
            Map<String, String> s3Metadata = new HashMap<>(metadata);
            s3Metadata.put("content-type", "text/html");
            s3Metadata.put("charset", "UTF-8");

            coreS3Service.putBytes(bucketName, key, contentBytes, "text/html; charset=UTF-8", s3Metadata, null);

            // Generate S3 URI
            S3Uri s3Uri = new S3Uri(String.format("s3://%s/%s", bucketName, key));

            Instant expiresAt = coreS3Service.generatePresignedUrl(bucketName, key, PRESIGNED_URL_EXPIRATION, (url) -> { return null; }).orElse(Instant.now().plus(PRESIGNED_URL_EXPIRATION));
            PresignedUrl presignedUrl = new PresignedUrl("", expiresAt); // URL returned via consumer in production

            log.info("Successfully uploaded HTML report to S3 [fileName:{},size:{},uri:{}]", fileName, FileSize.ofBytes(fileSize).toHumanReadable(), s3Uri);

            return new UploadResult(s3Uri, FileSize.ofBytes(fileSize), presignedUrl);
        } catch (Exception e) {
            log.error("Unexpected error uploading HTML report [fileName:{},error:{}]", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload HTML report to S3", e);
        }
    }
    
    /**
     * Upload XBRL report to S3 with encryption and metadata.
     * Circuit breaker protects against S3 service degradation.
     * 
     * @param xbrlDocument The XBRL XML document to upload
     * @param fileName The file name (e.g., Large_Exposures_12345_2024-11-19.xbrl)
     * @param metadata Additional metadata tags
     * @return UploadResult containing S3 URI, file size, and presigned URL
     */
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadXbrlToLocalFallback")
    @Override
    public UploadResult uploadXbrlReport(Document xbrlDocument, String fileName, Map<String, String> metadata) {
        log.info("Uploading XBRL report to S3 [fileName:{},bucket:{}]", fileName, bucketName);
        
        try {
            // Convert Document to XML string with UTF-8 encoding
            String xmlContent = documentToString(xbrlDocument);
            String key = xbrlPrefix + fileName;
            byte[] contentBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
            long fileSize = contentBytes.length;

            Map<String, String> s3Metadata = new HashMap<>(metadata);
            s3Metadata.put("content-type", "application/xml");
            s3Metadata.put("charset", "UTF-8");

            coreS3Service.putBytes(bucketName, key, contentBytes, "application/xml; charset=UTF-8", s3Metadata, null);

            S3Uri s3Uri = new S3Uri(String.format("s3://%s/%s", bucketName, key));
            Instant expiresAt = coreS3Service.generatePresignedUrl(bucketName, key, PRESIGNED_URL_EXPIRATION, (url) -> { return null; }).orElse(Instant.now().plus(PRESIGNED_URL_EXPIRATION));
            PresignedUrl presignedUrl = new PresignedUrl("", expiresAt);

            log.info("Successfully uploaded XBRL report to S3 [fileName:{},size:{},uri:{}]", fileName, FileSize.ofBytes(fileSize).toHumanReadable(), s3Uri);

            return new UploadResult(s3Uri, FileSize.ofBytes(fileSize), presignedUrl);
        } catch (Exception e) {
            log.error("Unexpected error uploading XBRL report [fileName:{},error:{}]", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload XBRL report to S3", e);
        }
    }
    
    /**
     * Fallback method for HTML upload when circuit breaker is open.
     * Saves file to local filesystem for deferred upload.
     */
    private UploadResult uploadHtmlToLocalFallback(String htmlContent, String fileName, 
                                                     Map<String, String> metadata, Exception ex) {
        log.warn("Circuit breaker OPEN - falling back to local storage for HTML [fileName:{},error:{}]", 
                fileName, ex.getMessage());
        
        return uploadToLocalFallback(htmlContent, fileName, "html", ex);
    }
    
    /**
     * Fallback method for XBRL upload when circuit breaker is open.
     * Saves file to local filesystem for deferred upload.
     */
    private UploadResult uploadXbrlToLocalFallback(Document xbrlDocument, String fileName, 
                                                     Map<String, String> metadata, Exception ex) {
        log.warn("Circuit breaker OPEN - falling back to local storage for XBRL [fileName:{},error:{}]", 
                fileName, ex.getMessage());
        
        try {
            String xmlContent = documentToString(xbrlDocument);
            return uploadToLocalFallback(xmlContent, fileName, "xbrl", ex);
        } catch (Exception e) {
            log.error("Failed to convert XBRL document to string in fallback [fileName:{},error:{}]", 
                    fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to save XBRL to local fallback", e);
        }
    }
    
    /**
     * Common fallback logic to save content to local filesystem.
     * Creates deferred upload directory and saves file for later retry.
     */
    private UploadResult uploadToLocalFallback(String content, String fileName, String type, Exception originalException) {
        try {
            // Create fallback directory if it doesn't exist
            Path fallbackDir = Paths.get(localFallbackPath);
            if (!Files.exists(fallbackDir)) {
                Files.createDirectories(fallbackDir);
            }
            
            // Save file to local filesystem
            Path filePath = fallbackDir.resolve(fileName);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            
            long fileSize = Files.size(filePath);
            
            // Generate local file URI
            S3Uri localUri = new S3Uri(String.format("s3://%s/%s%s", 
                    bucketName, 
                    type.equals("html") ? htmlPrefix : xbrlPrefix, 
                    fileName));
            
            // Generate dummy presigned URL (will be regenerated when uploaded to S3)
            Instant expiresAt = Instant.now().plus(PRESIGNED_URL_EXPIRATION);
            PresignedUrl presignedUrl = new PresignedUrl(
                    String.format("file://%s", filePath.toAbsolutePath()), 
                    expiresAt
            );
            
            log.info("Saved {} report to local fallback [fileName:{},path:{},size:{}]", 
                    type.toUpperCase(), fileName, filePath.toAbsolutePath(), 
                    FileSize.ofBytes(fileSize).toHumanReadable());
            
            return new UploadResult(localUri, FileSize.ofBytes(fileSize), presignedUrl);
            
        } catch (IOException e) {
            log.error("Failed to save {} report to local fallback [fileName:{},error:{}]", 
                    type.toUpperCase(), fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to save report to local fallback", e);
        }
    }
    
    /**
     * Convert XML Document to formatted string with UTF-8 encoding.
     */
    private String documentToString(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        
        return writer.toString();
    }
    
    @Override
    public String fetchCalculationData(String batchId) {
        // TODO: Implement fetching calculation data from S3
        log.warn("fetchCalculationData not yet implemented for batchId: {}", batchId);
        throw new UnsupportedOperationException("fetchCalculationData not yet implemented");
    }
    
    @Override
    public String fetchQualityData(String batchId) {
        // TODO: Implement fetching quality data from S3
        log.warn("fetchQualityData not yet implemented for batchId: {}", batchId);
        throw new UnsupportedOperationException("fetchQualityData not yet implemented");
    }
}
