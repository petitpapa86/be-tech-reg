package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Service for generating PDF reports from HTML content.
 */
public interface PdfReportGenerator {
    /**
     * Converts HTML content to PDF.
     *
     * @param htmlContent the HTML content to convert
     * @return Result containing the PDF bytes or an error
     */
    Result<byte[]> generateFromHtml(String htmlContent);
}
