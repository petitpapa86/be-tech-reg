package com.bcbs239.regtech.reportgeneration.infrastructure.pdf;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.domain.generation.PdfReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of PdfReportGenerator using Headless Chrome.
 * 
 * Uses the installed Chrome/Edge browser to render the HTML (including JS/CSS)
 * and print it to PDF.
 */
@Service
@Slf4j
public class ChromePdfReportGeneratorImpl implements PdfReportGenerator {

    private final String chromePath;
    private final int timeoutSeconds;

    public ChromePdfReportGeneratorImpl(
            @Value("${report-generation.pdf.chrome-path}") String chromePath,
            @Value("${report-generation.pdf.timeout-seconds:30}") int timeoutSeconds) {
        this.chromePath = chromePath;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Result<byte[]> generateFromHtml(String htmlContent) {
        Path tempHtml = null;
        Path tempPdf = null;
        
        try {
            // 1. Create temporary files
            tempHtml = Files.createTempFile("report_", ".html");
            tempPdf = Files.createTempFile("report_", ".pdf");
            
            // 2. Write HTML content to file
            Files.write(tempHtml, htmlContent.getBytes(StandardCharsets.UTF_8));
            
            // 3. Prepare Chrome command
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            command.add("--headless");
            command.add("--disable-gpu");
            command.add("--no-sandbox");
            command.add("--no-pdf-header-footer");
            command.add("--print-to-pdf=" + tempPdf.toAbsolutePath().toString());
            command.add(tempHtml.toAbsolutePath().toString());
            
            log.info("Executing Chrome PDF generation: {}", String.join(" ", command));
            
            // 4. Execute command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Merge stderr into stdout
            
            Process process = pb.start();
            
            // Capture output for debugging
            // In production, we might want to read this asynchronously to avoid blocking if buffer fills
            // But for short runs, it's usually okay. We'll ignore it for now or log if failed.
            
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return Result.failure(ErrorDetail.of(
                    "PDF_GENERATION_TIMEOUT", 
                    ErrorType.SYSTEM_ERROR, 
                    "PDF generation timed out after " + timeoutSeconds + " seconds",
                    "report.generation.pdf.timeout"
                ));
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("Chrome process failed with exit code {}. Output: {}", exitCode, output);
                return Result.failure(ErrorDetail.of(
                    "PDF_GENERATION_FAILED", 
                    ErrorType.SYSTEM_ERROR, 
                    "Chrome process failed with exit code " + exitCode,
                    "report.generation.pdf.failed"
                ));
            }
            
            // 5. Read PDF bytes
            if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                 return Result.failure(ErrorDetail.of(
                    "PDF_FILE_MISSING", 
                    ErrorType.SYSTEM_ERROR, 
                    "PDF file was not created by Chrome",
                    "report.generation.pdf.missing"
                ));
            }
            
            byte[] pdfBytes = Files.readAllBytes(tempPdf);
            log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);
            
            return Result.success(pdfBytes);
            
        } catch (IOException | InterruptedException e) {
            log.error("Error generating PDF", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Result.failure(ErrorDetail.of(
                "PDF_GENERATION_ERROR", 
                ErrorType.SYSTEM_ERROR, 
                "Error generating PDF: " + e.getMessage(),
                "report.generation.pdf.error"
            ));
        } finally {
            // 6. Cleanup
            cleanup(tempHtml);
            cleanup(tempPdf);
        }
    }
    
    private void cleanup(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }
}
