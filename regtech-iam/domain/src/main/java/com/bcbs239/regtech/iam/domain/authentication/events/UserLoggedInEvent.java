package com.bcbs239.regtech.iam.domain.authentication.events;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * UserLoggedInEvent - raised when a user successfully logs in
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoggedInEvent extends DomainEvent {

    private final UserId userId;
    private final Email email;
    private final Instant loginTime;
    private final String ipAddress;

    @JsonCreator
    public UserLoggedInEvent(
        @JsonProperty("userId") UserId userId,
        @JsonProperty("email") Email email,
        @JsonProperty("loginTime") Instant loginTime,
        @JsonProperty("ipAddress") String ipAddress
    ) {
        super(CorrelationContext.correlationId(), Maybe.none(), "UserLoggedInEvent");
        this.userId = userId;
        this.email = email;
        this.loginTime = loginTime;
        this.ipAddress = ipAddress;
    }

    @Override
    public String eventType() {
        return "UserLoggedInEvent";
    }
}
