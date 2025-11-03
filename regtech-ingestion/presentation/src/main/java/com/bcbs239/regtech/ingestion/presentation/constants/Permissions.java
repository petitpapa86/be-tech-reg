package com.bcbs239.regtech.modules.ingestion.presentation.constants;

/**
 * Constants for API endpoint permissions used in authorization.
 */
public final class Permissions {
    public static final String UPLOAD_FILE = "ingestion:upload";
    public static final String PROCESS_BATCH = "ingestion:process";
    public static final String VIEW_BATCH_STATUS = "ingestion:status:view";
    public static final String MANAGE_BATCHES = "ingestion:manage";
    
    private Permissions() {
        // Utility class
    }
}