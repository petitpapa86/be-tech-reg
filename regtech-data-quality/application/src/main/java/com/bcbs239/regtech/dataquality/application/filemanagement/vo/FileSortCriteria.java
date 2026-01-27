package com.bcbs239.regtech.dataquality.application.filemanagement.vo;

import org.springframework.data.domain.Sort;

/**
 * Value Object representing file sorting criteria.
 * Encapsulates the logic of mapping frontend sort keys to domain entity fields.
 */
public record FileSortCriteria(String field, Sort.Direction direction) {

    public static FileSortCriteria from(String sortBy, String sortOrder) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = mapSortField(sortBy);
        return new FileSortCriteria(field, direction);
    }

    public Sort toSpringSort() {
        return Sort.by(direction, field);
    }

    private static String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "upload_date" -> "createdAt";
            case "file_size" -> "fileSize";
            case "filename" -> "filename";
            case "status" -> "status";
            default -> "createdAt";
        };
    }
}
