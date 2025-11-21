package com.bcbs239.regtech.reportgeneration.infrastructure.filestorage;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri;
import com.bcbs239.regtech.reportgeneration.domain.storage.IReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Local filesystem storage service for deferred uploads.
 * 
 * This service saves reports to local filesystem when S3 is unavailable
 * (circuit breaker open). A scheduled job periodically retries uploading
 * deferred files to S3 when the service recovers.
 * 
 * Requirements: 12.5, 20.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService {
    
    private final S3ReportStorageService s3ReportStorageService;
    
    @Value("${report-generation.fallback.local-path:/tmp/deferred-reports/}")
    private String localFallbackPath;
    
    @Value("${report-generation.fallback.retry-interval-minutes:30}")
    private int retryIntervalMinutes;
    
    @Value("${report-generation.fallback.max-retry-attempts:5}")
    private int maxRetryAttempts;
    
    /**
     * Save HTML content to local filesystem for deferred upload.
     * 
     * @param htmlContent The HTML content to save
     * @param fileName The file name
     * @return Path to the saved file
     */
    public Path saveToLocal(String htmlContent, String fileName) throws IOException {
        Path fallbackDir = Paths.get(localFallbackPath);
        if (!Files.exists(fallbackDir)) {
            Files.createDirectories(fallbackDir);
        }
        
        Path filePath = fallbackDir.resolve(fileName);
        Files.writeString(filePath, htmlContent, StandardCharsets.UTF_8);
        
        // Create metadata file to track retry attempts
        createMetadataFile(filePath, "html");
        
        log.info("Saved HTML report to local fallback [fileName:{},path:{}]", 
                fileName, filePath.toAbsolutePath());
        
        return filePath;
    }
    
    /**
     * Save XBRL document to local filesystem for deferred upload.
     * 
     * @param xbrlDocument The XBRL document to save
     * @param fileName The file name
     * @return Path to the saved file
     */
    public Path saveToLocal(Document xbrlDocument, String fileName) throws Exception {
        String xmlContent = documentToString(xbrlDocument);
        
        Path fallbackDir = Paths.get(localFallbackPath);
        if (!Files.exists(fallbackDir)) {
            Files.createDirectories(fallbackDir);
        }
        
        Path filePath = fallbackDir.resolve(fileName);
        Files.writeString(filePath, xmlContent, StandardCharsets.UTF_8);
        
        // Create metadata file to track retry attempts
        createMetadataFile(filePath, "xbrl");
        
        log.info("Saved XBRL report to local fallback [fileName:{},path:{}]", 
                fileName, filePath.toAbsolutePath());
        
        return filePath;
    }
    
    /**
     * Scheduled job to retry uploading deferred files to S3.
     * Runs every configured interval (default: 30 minutes).
     * 
     * Requirements: 12.5, 20.1
     */
    @Scheduled(fixedDelayString = "${report-generation.fallback.retry-interval-minutes:30}000", 
               initialDelayString = "${report-generation.fallback.retry-interval-minutes:30}000")
    public void retryDeferredUploads() {
        log.info("Starting deferred upload retry job [fallbackPath:{}]", localFallbackPath);
        
        try {
            Path fallbackDir = Paths.get(localFallbackPath);
            if (!Files.exists(fallbackDir)) {
                log.debug("No deferred uploads directory found, skipping retry");
                return;
            }
            
            List<Path> deferredFiles = findDeferredFiles(fallbackDir);
            
            if (deferredFiles.isEmpty()) {
                log.debug("No deferred files found for retry");
                return;
            }
            
            log.info("Found {} deferred files to retry", deferredFiles.size());
            
            int successCount = 0;
            int failureCount = 0;
            int maxRetriesReached = 0;
            
            for (Path filePath : deferredFiles) {
                try {
                    DeferredFileMetadata metadata = readMetadata(filePath);
                    
                    // Check if max retries reached
                    if (metadata.retryCount >= maxRetryAttempts) {
                        log.warn("Max retry attempts reached for deferred file [file:{},attempts:{}]", 
                                filePath.getFileName(), metadata.retryCount);
                        maxRetriesReached++;
                        continue;
                    }
                    
                    // Attempt to upload to S3
                    boolean uploadSuccess = retryUpload(filePath, metadata);
                    
                    if (uploadSuccess) {
                        // Delete local file and metadata on successful upload
                        Files.deleteIfExists(filePath);
                        Files.deleteIfExists(getMetadataPath(filePath));
                        successCount++;
                        log.info("Successfully uploaded deferred file to S3 [file:{}]", 
                                filePath.getFileName());
                    } else {
                        // Increment retry count
                        updateMetadata(filePath, metadata.retryCount + 1);
                        failureCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing deferred file [file:{},error:{}]", 
                            filePath.getFileName(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            log.info("Deferred upload retry completed [success:{},failure:{},maxRetriesReached:{}]", 
                    successCount, failureCount, maxRetriesReached);
            
        } catch (Exception e) {
            log.error("Error in deferred upload retry job [error:{}]", e.getMessage(), e);
        }
    }
    
    /**
     * Find all deferred files in the fallback directory.
     */
    private List<Path> findDeferredFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{html,xml}")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(entry);
                }
            }
        }
        
        return files;
    }
    
    /**
     * Retry uploading a deferred file to S3.
     */
    private boolean retryUpload(Path filePath, DeferredFileMetadata metadata) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            String fileName = filePath.getFileName().toString();
            
            // Determine file type and upload
            if (metadata.fileType.equals("html")) {
                s3ReportStorageService.uploadHtmlReport(content, fileName, Map.of(
                        "deferred-upload", "true",
                        "original-timestamp", metadata.createdAt.toString(),
                        "retry-count", String.valueOf(metadata.retryCount)
                ));
            } else if (metadata.fileType.equals("xbrl")) {
                // For XBRL, we need to parse the XML string back to Document
                // For simplicity, we'll upload as raw content
                // In production, you might want to parse and validate
                s3ReportStorageService.uploadHtmlReport(content, fileName, Map.of(
                        "deferred-upload", "true",
                        "original-timestamp", metadata.createdAt.toString(),
                        "retry-count", String.valueOf(metadata.retryCount),
                        "content-type", "application/xml"
                ));
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Failed to retry upload for deferred file [file:{},error:{}]", 
                    filePath.getFileName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Create metadata file to track retry attempts.
     */
    private void createMetadataFile(Path filePath, String fileType) throws IOException {
        Path metadataPath = getMetadataPath(filePath);
        
        String metadata = String.format("%s|%s|%d", 
                fileType, 
                Instant.now().toString(), 
                0); // Initial retry count
        
        Files.writeString(metadataPath, metadata, StandardCharsets.UTF_8);
    }
    
    /**
     * Read metadata from metadata file.
     */
    private DeferredFileMetadata readMetadata(Path filePath) throws IOException {
        Path metadataPath = getMetadataPath(filePath);
        
        if (!Files.exists(metadataPath)) {
            // Create default metadata if missing
            return new DeferredFileMetadata("unknown", Instant.now(), 0);
        }
        
        String content = Files.readString(metadataPath, StandardCharsets.UTF_8);
        String[] parts = content.split("\\|");
        
        if (parts.length != 3) {
            return new DeferredFileMetadata("unknown", Instant.now(), 0);
        }
        
        return new DeferredFileMetadata(
                parts[0], 
                Instant.parse(parts[1]), 
                Integer.parseInt(parts[2])
        );
    }
    
    /**
     * Update metadata file with new retry count.
     */
    private void updateMetadata(Path filePath, int newRetryCount) throws IOException {
        DeferredFileMetadata metadata = readMetadata(filePath);
        Path metadataPath = getMetadataPath(filePath);
        
        String updatedMetadata = String.format("%s|%s|%d", 
                metadata.fileType, 
                metadata.createdAt.toString(), 
                newRetryCount);
        
        Files.writeString(metadataPath, updatedMetadata, StandardCharsets.UTF_8);
    }
    
    /**
     * Get metadata file path for a given file.
     */
    private Path getMetadataPath(Path filePath) {
        return filePath.getParent().resolve(filePath.getFileName() + ".meta");
    }
    
    /**
     * Convert XML Document to formatted string.
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
     * Get count of deferred files waiting for upload.
     */
    public int getDeferredFileCount() {
        try {
            Path fallbackDir = Paths.get(localFallbackPath);
            if (!Files.exists(fallbackDir)) {
                return 0;
            }
            
            return findDeferredFiles(fallbackDir).size();
            
        } catch (IOException e) {
            log.error("Error counting deferred files [error:{}]", e.getMessage());
            return -1;
        }
    }
    
    /**
     * Metadata for deferred file tracking.
     */
    private record DeferredFileMetadata(String fileType, Instant createdAt, int retryCount) {}
}
