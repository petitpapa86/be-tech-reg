package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class FiltersApplied {
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("dateFrom")
    private Instant dateFrom;
    
    @JsonProperty("dateTo")
    private Instant dateTo;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("search")
    private String search;

    public FiltersApplied(String status, Instant dateFrom, Instant dateTo, String format, String search) {
        this.status = status;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.format = format;
        this.search = search;
    }

    // Getters
    public String getStatus() { return status; }
    public Instant getDateFrom() { return dateFrom; }
    public Instant getDateTo() { return dateTo; }
    public String getFormat() { return format; }
    public String getSearch() { return search; }
}
