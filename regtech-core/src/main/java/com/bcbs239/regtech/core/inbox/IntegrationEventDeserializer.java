package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;

public interface IntegrationEventDeserializer {
    IntegrationEvent deserialize(String typeName, String json) throws Exception;
}

