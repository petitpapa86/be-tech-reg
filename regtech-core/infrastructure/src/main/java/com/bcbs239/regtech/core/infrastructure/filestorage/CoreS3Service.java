package com.bcbs239.regtech.core.infrastructure.filestorage;

import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Central S3 service used by other modules. Keeps S3 client creation and common behavior in core.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CoreS3Service {

    private final S3Properties s3Properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public PutObjectResponse putBytes(String bucket, String key, byte[] content, String contentType, Map<String, String> metadata, String kmsKeyId) {
        var putReq = S3Utils.buildPutObjectRequest(bucket, key, contentType, (long) content.length, metadata, kmsKeyId == null ? s3Properties.getKmsKeyId() : kmsKeyId);
        return s3Client.putObject(putReq, RequestBody.fromBytes(content));
    }

    public PutObjectResponse putString(String bucket, String key, String content, String contentType, Map<String, String> metadata, String kmsKeyId) {
        var safeContent = content == null ? "" : content;
        var bytes = safeContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var putReq = S3Utils.buildPutObjectRequest(bucket, key, contentType, (long) bytes.length, metadata, kmsKeyId == null ? s3Properties.getKmsKeyId() : kmsKeyId);
        return s3Client.putObject(putReq, RequestBody.fromString(safeContent, java.nio.charset.StandardCharsets.UTF_8));
    }

    public ResponseInputStream<GetObjectResponse> getObjectStream(String bucket, String key) {
        GetObjectRequest getReq = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return s3Client.getObject(getReq);
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
        return s3Client.headObject(req);
    }
}
