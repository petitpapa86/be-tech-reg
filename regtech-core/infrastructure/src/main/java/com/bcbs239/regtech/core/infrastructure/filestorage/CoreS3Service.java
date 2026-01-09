package com.bcbs239.regtech.core.infrastructure.filestorage;

import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central S3 service used by other modules. Keeps S3 client creation and common behavior in core.
 * Only created when S3 is enabled via ingestion.s3.enabled property.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.s3.enabled", havingValue = "true", matchIfMissing = true)
public class CoreS3Service {

    private final S3Properties s3Properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final ConcurrentHashMap<String, S3Client> regionClientCache = new ConcurrentHashMap<>();

    private static final Pattern EXPECTED_REGION_PATTERN = Pattern.compile("expecting '([a-z0-9-]+)'", Pattern.CASE_INSENSITIVE);

    private Optional<String> extractExpectedRegion(S3Exception e) {
        if (e == null) return Optional.empty();

        try {
            var details = e.awsErrorDetails();
            if (details != null) {
                String code = details.errorCode();
                if (code != null && !code.isBlank() && !"AuthorizationHeaderMalformed".equalsIgnoreCase(code)) {
                    return Optional.empty();
                }
            }
        } catch (Exception ignored) {
            // Fall back to message parsing
        }

        String message = e.getMessage();
        if (message == null || message.isBlank()) return Optional.empty();

        Matcher m = EXPECTED_REGION_PATTERN.matcher(message);
        if (!m.find()) return Optional.empty();

        String region = m.group(1);
        if (region == null || region.isBlank()) return Optional.empty();
        return Optional.of(region.trim());
    }

    private S3Client getOrCreateClientForRegion(String region) {
        String normalized = (region == null ? "" : region.trim());
        if (normalized.isEmpty()) {
            return s3Client;
        }

        return regionClientCache.computeIfAbsent(normalized, r -> {
            var builder = S3Client.builder().region(Region.of(r));

            if (s3Properties.getAccessKey() != null && !s3Properties.getAccessKey().trim().isEmpty()
                    && s3Properties.getSecretKey() != null && !s3Properties.getSecretKey().trim().isEmpty()) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey());
                builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            }

            if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().trim().isEmpty()) {
                builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
            }

            return builder.build();
        });
    }

    public PutObjectResponse putBytes(String bucket, String key, byte[] content, String contentType, Map<String, String> metadata, String kmsKeyId) {
        var putReq = S3Utils.buildPutObjectRequest(bucket, key, contentType, (long) content.length, metadata, kmsKeyId == null ? s3Properties.getKmsKeyId() : kmsKeyId);
        try {
            return s3Client.putObject(putReq, RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            Optional<String> expected = extractExpectedRegion(e);
            if (expected.isPresent() && (s3Properties.getRegion() == null || !expected.get().equalsIgnoreCase(s3Properties.getRegion()))) {
                log.warn("S3 region mismatch for putObject to s3://{}/{} (configuredRegion={}, expectedRegion={}) - retrying once with expected region",
                        bucket, key, s3Properties.getRegion(), expected.get());
                return getOrCreateClientForRegion(expected.get()).putObject(putReq, RequestBody.fromBytes(content));
            }
            throw e;
        }
    }

    public PutObjectResponse putString(String bucket, String key, String content, String contentType, Map<String, String> metadata, String kmsKeyId) {
        var safeContent = content == null ? "" : content;
        var bytes = safeContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var putReq = S3Utils.buildPutObjectRequest(bucket, key, contentType, (long) bytes.length, metadata, kmsKeyId == null ? s3Properties.getKmsKeyId() : kmsKeyId);
        try {
            return s3Client.putObject(putReq, RequestBody.fromString(safeContent, java.nio.charset.StandardCharsets.UTF_8));
        } catch (S3Exception e) {
            Optional<String> expected = extractExpectedRegion(e);
            if (expected.isPresent() && (s3Properties.getRegion() == null || !expected.get().equalsIgnoreCase(s3Properties.getRegion()))) {
                log.warn("S3 region mismatch for putObject to s3://{}/{} (configuredRegion={}, expectedRegion={}) - retrying once with expected region",
                        bucket, key, s3Properties.getRegion(), expected.get());
                return getOrCreateClientForRegion(expected.get()).putObject(putReq, RequestBody.fromString(safeContent, java.nio.charset.StandardCharsets.UTF_8));
            }
            throw e;
        }
    }

    public ResponseInputStream<GetObjectResponse> getObjectStream(String bucket, String key) {
        GetObjectRequest getReq = GetObjectRequest.builder().bucket(bucket).key(key).build();
        try {
            return s3Client.getObject(getReq);
        } catch (S3Exception e) {
            Optional<String> expected = extractExpectedRegion(e);
            if (expected.isPresent() && (s3Properties.getRegion() == null || !expected.get().equalsIgnoreCase(s3Properties.getRegion()))) {
                log.warn("S3 region mismatch for getObject s3://{}/{} (configuredRegion={}, expectedRegion={}) - retrying once with expected region",
                        bucket, key, s3Properties.getRegion(), expected.get());
                return getOrCreateClientForRegion(expected.get()).getObject(getReq);
            }
            throw e;
        }
    }

    public Optional<Instant> generatePresignedUrl(String bucket, String key, Duration expiration, java.util.function.Function<String, Void> urlConsumer) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().getObjectRequest(getObjectRequest).signatureDuration(expiration).build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            String url = presigned.url().toString();
            if (urlConsumer != null) urlConsumer.apply(url);
            return Optional.of(Instant.now().plus(expiration));
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for s3://{}/{}: {}", bucket, key, e.getMessage());
            return Optional.empty();
        }
    }

    public HeadObjectResponse headObject(String bucket, String key) {
        HeadObjectRequest req = HeadObjectRequest.builder().bucket(bucket).key(key).build();
        try {
            return s3Client.headObject(req);
        } catch (S3Exception e) {
            Optional<String> expected = extractExpectedRegion(e);
            if (expected.isPresent() && (s3Properties.getRegion() == null || !expected.get().equalsIgnoreCase(s3Properties.getRegion()))) {
                log.warn("S3 region mismatch for headObject s3://{}/{} (configuredRegion={}, expectedRegion={}) - retrying once with expected region",
                        bucket, key, s3Properties.getRegion(), expected.get());
                return getOrCreateClientForRegion(expected.get()).headObject(req);
            }
            throw e;
        }
    }

    public DeleteObjectResponse deleteObject(String bucket, String key) {
        DeleteObjectRequest deleteReq = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        try {
            return s3Client.deleteObject(deleteReq);
        } catch (S3Exception e) {
            Optional<String> expected = extractExpectedRegion(e);
            if (expected.isPresent() && (s3Properties.getRegion() == null || !expected.get().equalsIgnoreCase(s3Properties.getRegion()))) {
                log.warn("S3 region mismatch for deleteObject s3://{}/{} (configuredRegion={}, expectedRegion={}) - retrying once with expected region",
                        bucket, key, s3Properties.getRegion(), expected.get());
                return getOrCreateClientForRegion(expected.get()).deleteObject(deleteReq);
            }
            throw e;
        }
    }
}
