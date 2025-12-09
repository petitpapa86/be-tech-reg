package com.bcbs239.regtech.reportgeneration.infrastructure.filestorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File Path Resolver
 * 
 * Resolves file paths for calculation and quality data based on environment.
 * In production, returns S3 URIs. In development, returns local filesystem paths.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4
 */
@Component
@Slf4j
public class FilePathResolver {
    
    private final Environment environment;
    private final String s3Bucket;
    private final String calculationPrefix;
    private final String qualityPrefix;
    private final String localBasePath;
    
    public FilePathResolver(
            Environment environment,
            @Value("${report-generation.s3.bucket:risk-analysis}") String s3Bucket,
            @Value("${risk-calculation.storage.s3.prefix:calculations/}") String calculationPrefix,
            @Value("${data-quality.storage.s3.prefix:quality/}") String qualityPrefix,
            @Value("${report-generation.fallback.local-path:./data}") String localBasePath) {
        
        this.environment = environment;
        this.s3Bucket = s3Bucket;
        this.calculationPrefix = calculationPrefix;
        this.qualityPrefix = qualityPrefix;
        this.localBasePath = localBasePath;
    }
    
    /**
     * Resolve calculation data path
     * Production: s3://risk-analysis/calculations/calc_batch_{id}.json
     * Development: ./data/calculated/calc_batch_{id}.json
     */
    public String resolveCalculationPath(String batchId) {
        if (isProd()) {
            String key = calculationPrefix + "calc_batch_" + batchId + ".json";
            String s3Uri = "s3://" + s3Bucket + "/" + key;
            log.debug("Resolved calculation path (production): {}", s3Uri);
            return s3Uri;
        } else {
            Path localPath = Paths.get(localBasePath, "calculated", "calc_batch_" + batchId + ".json");
            log.debug("Resolved calculation path (development): {}", localPath);
            return localPath.toString();
        }
    }
    
    /**
     * Resolve quality data path
     * Production: s3://risk-analysis/quality/quality_batch_{id}.json
     * Development: ./data/quality/quality_batch_{id}.json
     */
    public String resolveQualityPath(String batchId) {
        if (isProd()) {
            String key = qualityPrefix + "quality_batch_" + batchId + ".json";
            String s3Uri = "s3://" + s3Bucket + "/" + key;
            log.debug("Resolved quality path (production): {}", s3Uri);
            return s3Uri;
        } else {
            Path localPath = Paths.get(localBasePath, "quality", "quality_batch_" + batchId + ".json");
            log.debug("Resolved quality path (development): {}", localPath);
            return localPath.toString();
        }
    }
    
    /**
     * Check if running in production environment
     */
    public boolean isProd() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse S3 URI into bucket and key components
     */
    public static S3Location parseS3Uri(String s3Uri) {
        if (s3Uri == null || !s3Uri.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 URI: " + s3Uri);
        }
        
        String withoutProtocol = s3Uri.substring(5); // Remove "s3://"
        int firstSlash = withoutProtocol.indexOf('/');
        
        if (firstSlash == -1) {
            throw new IllegalArgumentException("Invalid S3 URI format: " + s3Uri);
        }
        
        String bucket = withoutProtocol.substring(0, firstSlash);
        String key = withoutProtocol.substring(firstSlash + 1);
        
        return new S3Location(bucket, key);
    }
    
    /**
     * S3 location holder
     */
    public record S3Location(String bucket, String key) {}
}
