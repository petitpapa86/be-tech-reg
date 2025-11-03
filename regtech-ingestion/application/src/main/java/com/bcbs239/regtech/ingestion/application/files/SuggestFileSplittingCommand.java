package com.bcbs239.regtech.ingestion.application.files;

import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;

public record SuggestFileSplittingCommand(FileMetadata fileMetadata, String authToken) {
}

