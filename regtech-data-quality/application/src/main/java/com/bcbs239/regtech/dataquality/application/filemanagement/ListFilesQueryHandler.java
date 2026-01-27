package com.bcbs239.regtech.dataquality.application.filemanagement;

import com.bcbs239.regtech.dataquality.application.filemanagement.mapper.FileResponseMapper;
import com.bcbs239.regtech.dataquality.application.filemanagement.vo.FileSortCriteria;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FileListResponse;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FileResponse;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FiltersApplied;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.PaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for ListFilesQuery.
 */
@Component
public class ListFilesQueryHandler {

    private final IQualityReportRepository qualityReportRepository;
    private final FileResponseMapper fileResponseMapper;

    public ListFilesQueryHandler(IQualityReportRepository qualityReportRepository, FileResponseMapper fileResponseMapper) {
        this.qualityReportRepository = qualityReportRepository;
        this.fileResponseMapper = fileResponseMapper;
    }

    public FileListResponse handle(ListFilesQuery query) {
        // Prepare filters
        QualityStatus status = QualityStatus.from(query.status());
        
        // Prepare Pageable
        // Note: query.page() is 1-based from controller usually, but Spring PageRequest is 0-based.
        int pageIndex = Math.max(0, query.page() - 1);
        
        // Build sort using Value Object
        FileSortCriteria sortCriteria = FileSortCriteria.from(query.sortBy(), query.sortOrder());
        Pageable pageable = PageRequest.of(pageIndex, query.size(), sortCriteria.toSpringSort());

        // Execute query
        Page<QualityReport> page = qualityReportRepository.findWithFilters(
            query.bankId(),
            status,
            query.dateFrom(),
            query.format(),
            query.searchQuery(),
            pageable
        );

        // Map results
        List<FileResponse> fileResponses = page.getContent().stream()
            .map(fileResponseMapper::toDto)
            .collect(Collectors.toList());

        // Create pagination response
        PaginationResponse pagination = new PaginationResponse(
            page.getNumber() + 1, // Return 1-based page number
            page.getSize(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );

        // Create filters response
        FiltersApplied filters = new FiltersApplied(
            query.status(),
            query.dateFrom(),
            null, // dateTo not implemented in query yet
            query.format(),
            query.searchQuery()
        );

        return new FileListResponse(fileResponses, pagination, filters);
    }
}
