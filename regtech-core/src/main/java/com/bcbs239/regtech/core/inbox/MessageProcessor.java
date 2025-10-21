package com.bcbs239.regtech.core.inbox;

public interface MessageProcessor {
    void process(InboxMessageEntity message);
}

