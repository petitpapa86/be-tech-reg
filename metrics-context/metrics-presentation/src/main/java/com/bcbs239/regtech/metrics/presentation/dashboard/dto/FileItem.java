package com.bcbs239.regtech.metrics.presentation.dashboard.dto;

public class FileItem {
    public final String filename;
    public final String date;
    public final Double score;
    public final String status;

    public FileItem(String filename, String date, Double score, String status) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
    }
}
