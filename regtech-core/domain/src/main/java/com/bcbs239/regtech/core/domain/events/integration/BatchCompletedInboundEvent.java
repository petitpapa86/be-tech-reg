package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class BatchCompletedInboundEvent extends DomainEvent {
    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3Uri;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    private final String eventVersion;
    private final String filename;

    @JsonCreator
    public BatchCompletedInboundEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3Uri") String s3Uri,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("fileSizeBytes") long fileSizeBytes,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("filename") String filename){
        super(batchId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Uri = s3Uri;
        this.totalExposures = totalExposures;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
        this.filename = filename;
    }



    //verify event fields are correctly  set
    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
                && bankId != null && !bankId.isEmpty()
                && s3Uri != null && !s3Uri.isEmpty()
                && filename != null && !filename.isEmpty()
                && totalExposures >= 0
                && fileSizeBytes >= 0;
    }
}
