package com.bcbs239.regtech.modules.ingestion.application.files;

import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;

public record SuggestFileSplittingCommand(FileMetadata fileMetadata, String authToken) {
}

