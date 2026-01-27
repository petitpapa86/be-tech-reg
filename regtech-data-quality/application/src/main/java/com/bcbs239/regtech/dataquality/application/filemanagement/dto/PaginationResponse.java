package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginationResponse {
    @JsonProperty("currentPage")
    private int currentPage;
    
    @JsonProperty("pageSize")
    private int pageSize;
    
    @JsonProperty("totalPages")
    private int totalPages;
    
    @JsonProperty("totalItems")
    private long totalItems;
    
    @JsonProperty("hasNext")
    private boolean hasNext;
    
    @JsonProperty("hasPrevious")
    private boolean hasPrevious;

    public PaginationResponse(int currentPage, int pageSize, int totalPages, long totalItems, boolean hasNext, boolean hasPrevious) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    // Getters
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
    public long getTotalItems() { return totalItems; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
}
