package com.bcbs239.regtech.iam.domain.authentication.events;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.authentication.RefreshTokenId;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * RefreshTokenRevokedEvent - raised when a refresh token is revoked
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshTokenRevokedEvent extends DomainEvent {

    private final RefreshTokenId tokenId;
    private final UserId userId;
    private final Instant revokedAt;

    @JsonCreator
    public RefreshTokenRevokedEvent(
        @JsonProperty("tokenId") RefreshTokenId tokenId,
        @JsonProperty("userId") UserId userId,
        @JsonProperty("revokedAt") Instant revokedAt
    ) {
        super(CorrelationContext.correlationId());
        this.tokenId = tokenId;
        this.userId = userId;
        this.revokedAt = revokedAt;
    }

}
