package com.bcbs239.regtech.core.domain.shared.dto;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;

public record ParsedBatchData(BatchDataDTO batchData,
                              BankInfo bankInfo) {
}
