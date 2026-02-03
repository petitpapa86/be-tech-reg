package com.bcbs239.regtech.metrics.presentation.dashboard.dto;

public class FileItem {
    public final String id;
    public final String filename;
    public final String date;
    public final Double score;
    public final String status;
    public final String reportId;

    public FileItem(String id, String filename, String date, Double score, String status, String reportId) {
        this.id = id;
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
        this.reportId = reportId;
    }
}
