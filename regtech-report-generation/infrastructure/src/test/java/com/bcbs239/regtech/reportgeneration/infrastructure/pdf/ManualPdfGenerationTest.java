package com.bcbs239.regtech.reportgeneration.infrastructure.pdf;

import com.bcbs239.regtech.core.domain.shared.Result;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manual tool to generate PDF from HTML using the same logic as the application.
 * Run this test to generate a PDF from the existing HTML file.
 */
public class ManualPdfGenerationTest {
    
    // Default paths based on your environment
    private static final String DEFAULT_CHROME_PATH = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    private static final String DEFAULT_INPUT_PATH = "C:\\Users\\alseny\\Desktop\\react projects\\regtech\\data\\reports\\html\\Comprehensive_Risk_Analysis_08081_2026-01-28.html";
    private static final String DEFAULT_OUTPUT_PATH = "C:\\Users\\alseny\\Desktop\\react projects\\regtech\\data\\reports\\pdf\\manual_output_test.pdf";

    @Test
    public void generatePdfFromExistingHtml() {
        try {
            String inputPath = System.getProperty("pdf.input", DEFAULT_INPUT_PATH);
            String outputPath = System.getProperty("pdf.output", DEFAULT_OUTPUT_PATH);
            String chromePath = System.getProperty("pdf.chrome", DEFAULT_CHROME_PATH);

            System.out.println("==========================================");
            System.out.println("MANUAL PDF GENERATION TEST");
            System.out.println("==========================================");
            System.out.println("Input HTML:  " + inputPath);
            System.out.println("Output PDF:  " + outputPath);
            
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("WARNING: Input file not found: " + inputPath);
                System.out.println("Skipping PDF generation test.");
                return; // Skip test if file doesn't exist
            }

            System.out.println("Reading HTML content...");
            String htmlContent = Files.readString(Paths.get(inputPath));

            System.out.println("Initializing PDF Generator...");
            ChromePdfReportGeneratorImpl generator = new ChromePdfReportGeneratorImpl(chromePath, 60);

            System.out.println("Generating PDF (this may take a few seconds)...");
            Result<byte[]> result = generator.generateFromHtml(htmlContent);

            if (result.isSuccess()) {
                System.out.println("Writing output file...");
                Path outputFilePath = Paths.get(outputPath);
                // Create parent directories if needed
                if (outputFilePath.getParent() != null) {
                    Files.createDirectories(outputFilePath.getParent());
                }
                Files.write(outputFilePath, result.getValueOrThrow());
                System.out.println("SUCCESS! PDF created at: " + outputPath);
            } else {
                var error = result.getError().orElseThrow(() -> new RuntimeException("Unknown error"));
                throw new RuntimeException("FAILURE: " + error.getMessage() + " - " + error.getCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed", e);
        }
    }
}
