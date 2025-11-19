package com.bcbs239.regtech.reportgeneration.infrastructure.filestorage;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

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
public class S3ReportStorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
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
     * @return S3UploadResult containing S3 URI, file size, and presigned URL
     */
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadHtmlToLocalFallback")
    public S3UploadResult uploadHtmlReport(String htmlContent, String fileName, Map<String, String> metadata) {
        log.info("Uploading HTML report to S3 [fileName:{},bucket:{}]", fileName, bucketName);
        
        try {
            String key = htmlPrefix + fileName;
            byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            long fileSize = contentBytes.length;
            
            // Prepare metadata
            Map<String, String> s3Metadata = new HashMap<>(metadata);
            s3Metadata.put("content-type", "text/html");
            s3Metadata.put("charset", "UTF-8");
            
            // Upload to S3 with encryption
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/html; charset=UTF-8")
                    .contentLength((long) contentBytes.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(s3Metadata)
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));
            
            // Generate S3 URI
            S3Uri s3Uri = new S3Uri(String.format("s3://%s/%s", bucketName, key));
            
            // Generate presigned URL
            PresignedUrl presignedUrl = generatePresignedUrl(s3Uri, PRESIGNED_URL_EXPIRATION);
            
            log.info("Successfully uploaded HTML report to S3 [fileName:{},size:{},uri:{}]", 
                    fileName, FileSize.ofBytes(fileSize).toHumanReadable(), s3Uri);
            
            return new S3UploadResult(s3Uri, FileSize.ofBytes(fileSize), presignedUrl);
            
        } catch (S3Exception e) {
            log.error("S3 error uploading HTML report [fileName:{},error:{},statusCode:{}]", 
                    fileName, e.getMessage(), e.statusCode(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading HTML report [fileName:{},error:{}]", 
                    fileName, e.getMessage(), e);
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
     * @return S3UploadResult containing S3 URI, file size, and presigned URL
     */
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadXbrlToLocalFallback")
    public S3UploadResult uploadXbrlReport(Document xbrlDocument, String fileName, Map<String, String> metadata) {
        log.info("Uploading XBRL report to S3 [fileName:{},bucket:{}]", fileName, bucketName);
        
        try {
            // Convert Document to XML string with UTF-8 encoding
            String xmlContent = documentToString(xbrlDocument);
            String key = xbrlPrefix + fileName;
            byte[] contentBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
            long fileSize = contentBytes.length;
            
            // Prepare metadata
            Map<String, String> s3Metadata = new HashMap<>(metadata);
            s3Metadata.put("content-type", "application/xml");
            s3Metadata.put("charset", "UTF-8");
            
            // Upload to S3 with encryption
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/xml; charset=UTF-8")
                    .contentLength((long) contentBytes.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(s3Metadata)
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));
            
            // Generate S3 URI
            S3Uri s3Uri = new S3Uri(String.format("s3://%s/%s", bucketName, key));
            
            // Generate presigned URL
            PresignedUrl presignedUrl = generatePresignedUrl(s3Uri, PRESIGNED_URL_EXPIRATION);
            
            log.info("Successfully uploaded XBRL report to S3 [fileName:{},size:{},uri:{}]", 
                    fileName, FileSize.ofBytes(fileSize).toHumanReadable(), s3Uri);
            
            return new S3UploadResult(s3Uri, FileSize.ofBytes(fileSize), presignedUrl);
            
        } catch (S3Exception e) {
            log.error("S3 error uploading XBRL report [fileName:{},error:{},statusCode:{}]", 
                    fileName, e.getMessage(), e.statusCode(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading XBRL report [fileName:{},error:{}]", 
                    fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload XBRL report to S3", e);
        }
    }
    
    /**
     * Generate presigned URL for temporary authenticated access to S3 object.
     * 
     * @param s3Uri The S3 URI of the object
     * @param expiration Duration until URL expires
     * @return PresignedUrl with expiration tracking
     */
    public PresignedUrl generatePresignedUrl(S3Uri s3Uri, Duration expiration) {
        log.debug("Generating presigned URL [uri:{},expiration:{}]", s3Uri, expiration);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Uri.getBucket())
                    .key(s3Uri.getKey())
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            
            Instant expiresAt = Instant.now().plus(expiration);
            PresignedUrl presignedUrl = new PresignedUrl(presignedRequest.url().toString(), expiresAt);
            
            log.debug("Generated presigned URL [uri:{},expiresAt:{}]", s3Uri, expiresAt);
            
            return presignedUrl;
            
        } catch (Exception e) {
            log.error("Failed to generate presigned URL [uri:{},error:{}]", s3Uri, e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
    
    /**
     * Fallback method for HTML upload when circuit breaker is open.
     * Saves file to local filesystem for deferred upload.
     */
    private S3UploadResult uploadHtmlToLocalFallback(String htmlContent, String fileName, 
                                                     Map<String, String> metadata, Exception ex) {
        log.warn("Circuit breaker OPEN - falling back to local storage for HTML [fileName:{},error:{}]", 
                fileName, ex.getMessage());
        
        return uploadToLocalFallback(htmlContent, fileName, "html", ex);
    }
    
    /**
     * Fallback method for XBRL upload when circuit breaker is open.
     * Saves file to local filesystem for deferred upload.
     */
    private S3UploadResult uploadXbrlToLocalFallback(Document xbrlDocument, String fileName, 
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
    private S3UploadResult uploadToLocalFallback(String content, String fileName, String type, Exception originalException) {
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
            
            return new S3UploadResult(localUri, FileSize.ofBytes(fileSize), presignedUrl);
            
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
    
    /**
     * Result object containing S3 upload information.
     */
    public record S3UploadResult(S3Uri s3Uri, FileSize fileSize, PresignedUrl presignedUrl) {}
}
