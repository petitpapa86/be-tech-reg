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
 * RefreshTokenCreatedEvent - raised when a refresh token is created
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshTokenCreatedEvent extends DomainEvent {

    private final RefreshTokenId tokenId;
    private final UserId userId;
    private final Instant expiresAt;

    @JsonCreator
    public RefreshTokenCreatedEvent(
        @JsonProperty("tokenId") RefreshTokenId tokenId,
        @JsonProperty("userId") UserId userId,
        @JsonProperty("expiresAt") Instant expiresAt
    ) {
        super(CorrelationContext.correlationId(), Maybe.none(), "RefreshTokenCreatedEvent");
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    @Override
    public String eventType() {
        return "RefreshTokenCreatedEvent";
    }
}
