package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FileListResponse {
    @JsonProperty("data")
    private List<FileResponse> data;
    
    @JsonProperty("pagination")
    private PaginationResponse pagination;
    
    @JsonProperty("filters")
    private FiltersApplied filters;

    public FileListResponse(List<FileResponse> data, PaginationResponse pagination, FiltersApplied filters) {
        this.data = data;
        this.pagination = pagination;
        this.filters = filters;
    }

    // Getters
    public List<FileResponse> getData() { return data; }
    public PaginationResponse getPagination() { return pagination; }
    public FiltersApplied getFilters() { return filters; }
}
