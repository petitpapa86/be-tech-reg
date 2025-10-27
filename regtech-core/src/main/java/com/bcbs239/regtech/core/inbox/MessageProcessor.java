package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.shared.Result;

public interface MessageProcessor {
    Result<Void> process(InboxMessageEntity message) throws Exception;
}

