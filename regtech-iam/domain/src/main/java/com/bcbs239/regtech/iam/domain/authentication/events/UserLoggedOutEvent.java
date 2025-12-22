package com.bcbs239.regtech.iam.domain.authentication.events;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * UserLoggedOutEvent - raised when a user logs out
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoggedOutEvent extends DomainEvent {

    private final UserId userId;
    private final Instant logoutTime;

    @JsonCreator
    public UserLoggedOutEvent(
        @JsonProperty("userId") UserId userId,
        @JsonProperty("logoutTime") Instant logoutTime
    ) {
        super(CorrelationContext.correlationId());
        this.userId = userId;
        this.logoutTime = logoutTime;
    }

}
