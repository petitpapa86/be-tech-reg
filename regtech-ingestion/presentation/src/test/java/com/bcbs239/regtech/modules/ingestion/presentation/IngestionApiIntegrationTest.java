package com.bcbs239.regtech.modules.ingestion.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the Ingestion API endpoints focusing on
 * file upload and S3 storage functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ingestion File Upload API Integration Test")
class IngestionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should upload Community First Bank loan portfolio file to S3 via API")
    void shouldUploadLoanPortfolioFileToS3ViaApi() throws Exception {
        // Given: Sample loan portfolio file
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "daily_loans_2024_09_12.json",
            MediaType.APPLICATION_JSON_VALUE,
            inputFile.getInputStream()
        );

        // When & Then: Upload file via API (focus on S3 storage)
        mockMvc.perform(multipart("/api/v1/ingestion/upload")
                .file(file)
                .param("bankId", "COMMUNITY_FIRST_BANK"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.s3Reference").exists())
                .andExpect(jsonPath("$.s3Reference.bucket").value("regtech-data-storage"))
                .andExpect(jsonPath("$.s3Reference.key").exists())
                .andExpect(jsonPath("$.s3Reference.key").value(org.hamcrest.Matchers.startsWith("raw/")))
                .andExpect(jsonPath("$.fileMetadata").exists())
                .andExpect(jsonPath("$.fileMetadata.originalFilename").value("daily_loans_2024_09_12.json"))
                .andExpect(jsonPath("$.fileMetadata.contentType").value("application/json"))
                .andExpect(jsonPath("$.fileMetadata.fileSizeBytes").exists())
                .andExpect(jsonPath("$.message").value("File uploaded successfully to S3"));
    }

    @Test
    @DisplayName("Should upload enhanced loan portfolio file to S3 via API")
    void shouldUploadEnhancedLoanPortfolioFileToS3ViaApi() throws Exception {
        // Given: Enhanced loan portfolio file
        ClassPathResource inputFile = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "enhanced_daily_loans_2024_09_12.json",
            MediaType.APPLICATION_JSON_VALUE,
            inputFile.getInputStream()
        );

        // When & Then: Upload enhanced file via API
        mockMvc.perform(multipart("/api/v1/ingestion/upload")
                .file(file)
                .param("bankId", "COMMUNITY_FIRST_BANK"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.s3Reference").exists())
                .andExpect(jsonPath("$.s3Reference.key").value(org.hamcrest.Matchers.containsString("enhanced_daily_loans")))
                .andExpect(jsonPath("$.fileMetadata.originalFilename").value("enhanced_daily_loans_2024_09_12.json"))
                .andExpect(jsonPath("$.message").value("File uploaded successfully to S3"));
    }

    @Test
    @DisplayName("Should retrieve batch status after upload")
    void shouldRetrieveBatchStatusAfterUpload() throws Exception {
        // Given: A batch ID (would be from previous upload)
        String batchId = "BATCH_001_COMMUNITY_FIRST_BANK";

        // When & Then: Check batch status (focus on upload status, not validation)
        mockMvc.perform(get("/api/v1/ingestion/batch/{batchId}/status", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.s3Reference").exists())
                .andExpect(jsonPath("$.s3Reference.bucket").exists())
                .andExpect(jsonPath("$.s3Reference.key").exists())
                .andExpect(jsonPath("$.fileMetadata").exists())
                .andExpect(jsonPath("$.uploadTimestamp").exists())
                .andExpect(jsonPath("$.bankId").value("COMMUNITY_FIRST_BANK"));
    }

    @Test
    @DisplayName("Should handle file format validation during upload")
    void shouldHandleFileFormatValidationDuringUpload() throws Exception {
        // Given: Invalid file (not JSON)
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "invalid.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "This is not a valid JSON file".getBytes()
        );

        // When & Then: Upload invalid file (basic format check only)
        mockMvc.perform(multipart("/api/v1/ingestion/upload")
                .file(invalidFile)
                .param("bankId", "COMMUNITY_FIRST_BANK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.message").value("File format not supported. Expected: JSON, Excel"))
                .andExpect(jsonPath("$.supportedFormats").isArray());
    }

    @Test
    @DisplayName("Should handle missing bank ID parameter")
    void shouldHandleMissingBankIdParameter() throws Exception {
        // Given: Valid file but missing bank ID
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "daily_loans_2024_09_12.json",
            MediaType.APPLICATION_JSON_VALUE,
            inputFile.getInputStream()
        );

        // When & Then: Upload without bank ID
        mockMvc.perform(multipart("/api/v1/ingestion/upload")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_REQUIRED_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Bank ID is required"))
                .andExpect(jsonPath("$.parameter").value("bankId"));
    }
}