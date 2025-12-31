package com.bcbs239.regtech.metrics.presentation.dashboard.dto;

public class ReportItem {
    public final String filename;
    public final String status;
    public final String details;

    public ReportItem(String filename, String status, String details) {
        this.filename = filename;
        this.status = status;
        this.details = details;
    }
}
