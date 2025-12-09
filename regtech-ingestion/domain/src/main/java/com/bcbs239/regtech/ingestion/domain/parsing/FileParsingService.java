package com.bcbs239.regtech.ingestion.domain.parsing;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.file.FileContent;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;

/**
 * Domain service interface for parsing file content.
 * Converts FileContent into structured ParsedFileData.
 */
public interface FileParsingService {
    
    /**
     * Parse file content based on the detected format.
     * 
     * @param fileContent the file content to parse
     * @return Result containing parsed data or error
     */
    Result<ParsedFileData> parseFileContent(FileContent fileContent);
}
