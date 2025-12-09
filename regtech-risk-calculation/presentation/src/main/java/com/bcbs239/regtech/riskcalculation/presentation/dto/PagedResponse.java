package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic DTO for paginated results.
 * Provides consistent pagination metadata across all endpoints.
 * 
 * @param <T> The type of content in the page
 * 
 * Requirements: 1.1, 1.3, 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    
    @NotNull(message = "Content is required")
    @JsonProperty("content")
    private List<T> content;
    
    @NotNull(message = "Page number is required")
    @Min(value = 0, message = "Page number must be non-negative")
    @JsonProperty("page")
    private Integer page;
    
    @NotNull(message = "Page size is required")
    @Min(value = 1, message = "Page size must be at least 1")
    @JsonProperty("size")
    private Integer size;
    
    @NotNull(message = "Total elements is required")
    @Min(value = 0, message = "Total elements must be non-negative")
    @JsonProperty("total_elements")
    private Long totalElements;
    
    @NotNull(message = "Total pages is required")
    @Min(value = 0, message = "Total pages must be non-negative")
    @JsonProperty("total_pages")
    private Integer totalPages;
    
    @JsonProperty("is_first")
    private boolean isFirst;
    
    @JsonProperty("is_last")
    private boolean isLast;
    
    @JsonProperty("has_next")
    private boolean hasNext;
    
    @JsonProperty("has_previous")
    private boolean hasPrevious;
    
    /**
     * Creates a PagedResponse from content and pagination metadata.
     * 
     * @param content The list of items in this page
     * @param page The current page number (0-indexed)
     * @param size The page size
     * @param totalElements The total number of elements across all pages
     * @param <T> The type of content
     * @return A fully populated PagedResponse
     */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PagedResponse.<T>builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .isFirst(page == 0)
            .isLast(page >= totalPages - 1)
            .hasNext(page < totalPages - 1)
            .hasPrevious(page > 0)
            .build();
    }
}
