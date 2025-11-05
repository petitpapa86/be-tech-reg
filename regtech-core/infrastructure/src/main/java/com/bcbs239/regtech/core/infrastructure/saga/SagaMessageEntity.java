package com.bcbs239.regtech.core.infrastructure.saga;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

import com.bcbs239.regtech.core.domain.saga.SagaMessage;

/**
 * Infrastructure implementation of SagaMessage.
 * Message exchanged between saga participants in different bounded contexts.
 * Supports both commands (requests for action) and events (notifications of state changes).
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class SagaMessageEntity extends SagaMessage {
