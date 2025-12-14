package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.dto.ParsedBatchData;

public interface BatchDataParsing {
    public ParsedBatchData parseBatchData(String jsonContent);
}
