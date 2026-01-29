package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResultsReader;
import com.bcbs239.regtech.dataquality.domain.model.reporting.StoredValidationResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Infrastructure adapter that loads detailed validation results.
 *
 * <p>Uses CoreS3Service for S3 access (if available) and standard File IO for local access.</p>
 * <p>Supports streaming JSON parsing to handle large files efficiently.</p>
 */
@Component
public class LocalDetailedResultsReader implements StoredValidationResultsReader {

    private static final Logger logger = LoggerFactory.getLogger(LocalDetailedResultsReader.class);

    private final Optional<CoreS3Service> coreS3Service;
    private final ObjectMapper objectMapper;
    private final String localBasePath;

    public LocalDetailedResultsReader(
        Optional<CoreS3Service> coreS3Service,
        ObjectMapper objectMapper,
        @Value("${data-quality.storage.local.base-path:${user.dir}/data/quality}") String localBasePath
    ) {
        this.coreS3Service = coreS3Service;
        this.objectMapper = objectMapper;
        this.localBasePath = localBasePath;
    }

    @Override
    public Optional<StoredValidationResults> load(String detailsUri) {
        if (detailsUri == null || detailsUri.isBlank()) {
            throw new IllegalArgumentException("detailsUri cannot be null or empty");
        }

        try {
            StoredValidationResults results;
            // Handle S3 URIs
            if (detailsUri.startsWith("s3://") && !detailsUri.startsWith("s3://local/")) {
                results = loadFromS3(detailsUri);
            } else {
                // Handle Local URIs
                results = loadFromLocal(detailsUri);
            }
            return Optional.ofNullable(results);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Wrap checked exceptions (including FileNotFoundException) in RuntimeException
            // to allow them to propagate without changing the interface signature to throw checked exceptions.
            // This preserves the intent of "throwing" rather than returning null.
            throw new RuntimeException("Error loading validation results from " + detailsUri, e);
        }
    }

    private StoredValidationResults loadFromS3(String uri) throws Exception {
        if (coreS3Service.isEmpty()) {
            throw new IllegalStateException("S3 service not available");
        }

        var parsed = S3Utils.parseS3Uri(uri);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 URI: " + uri);
        }

        try (ResponseInputStream<GetObjectResponse> s3Stream = coreS3Service.get().getObjectStream(parsed.get().bucket(), parsed.get().key())) {
            return objectMapper.readValue(s3Stream, StoredValidationResults.class);
        }
    }

    private StoredValidationResults loadFromLocal(String uri) throws Exception {
        
        // Handle legacy s3://local/ mapping
        if (uri.startsWith("s3://local/")) {
            String key = uri.substring("s3://local/".length());
            Path path = Paths.get(localBasePath, key);
            
            // Fallback: If running from a module directory (e.g. regtech-data-quality),
            // the file might be in the project root's data directory.
            if (!Files.exists(path)) {
                Path currentDir = Paths.get(System.getProperty("user.dir"));
                if (currentDir.getFileName().toString().startsWith("regtech-")) {
                    Path projectRoot = currentDir.getParent();
                    if (projectRoot != null) {
                        // Try to find the file in ../data/quality/{key}
                        // We assume the standard structure is data/quality relative to root
                        Path fallbackPath = projectRoot.resolve("data/quality").resolve(key);
                        if (Files.exists(fallbackPath)) {
                            logger.info("File not found at {}, found at fallback: {}", path, fallbackPath);
                            return loadFromPath(fallbackPath);
                        }
                    }
                }
            }
            return loadFromPath(path);
        }

        String pathStr = uri;
        if (uri.startsWith("file://")) {
            pathStr = uri.substring(7);
        } else if (uri.startsWith("file:")) {
            pathStr = uri.substring(5);
        }
        
        // Handle Windows /C:/ paths
        if (pathStr.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
             if (pathStr.length() > 2 && pathStr.charAt(2) == ':') {
                 pathStr = pathStr.substring(1);
             }
        }
        
        pathStr = URLDecoder.decode(pathStr, StandardCharsets.UTF_8);
        return loadFromPath(Paths.get(pathStr));
    }

    private StoredValidationResults loadFromPath(Path path) throws Exception {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + path);
        }
        try (InputStream is = Files.newInputStream(path)) {
            return objectMapper.readValue(is, StoredValidationResults.class);
        }
    }
}
