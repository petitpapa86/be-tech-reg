package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class FileListResponse {
    @JsonProperty("files")
    private List<FileResponse> files;
    
    @JsonProperty("pagination")
    private PaginationResponse pagination;
    
    @JsonProperty("filters")
    private FiltersApplied filters;

    public FileListResponse(List<FileResponse> data, PaginationResponse pagination, FiltersApplied filters) {
        this.files = data;
        this.pagination = pagination;
        this.filters = filters;
    }

}
