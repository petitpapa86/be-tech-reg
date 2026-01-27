package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class FileResponse {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("uploadDate")
    private Instant uploadDate;
    
    @JsonProperty("size")
    private long size;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("bankId")
    private String bankId;
    
    @JsonProperty("batchId")
    private String batchId;

    public FileResponse(String id, String filename, Instant uploadDate, long size, String status, String format, String bankId, String batchId) {
        this.id = id;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.size = size;
        this.status = status;
        this.format = format;
        this.bankId = bankId;
        this.batchId = batchId;
    }

    // Getters
    public String getId() { return id; }
    public String getFilename() { return filename; }
    public Instant getUploadDate() { return uploadDate; }
    public long getSize() { return size; }
    public String getStatus() { return status; }
    public String getFormat() { return format; }
    public String getBankId() { return bankId; }
    public String getBatchId() { return batchId; }
}
