package com.bcbs239.regtech.modules.ingestion.presentation.batch.suggestion;

import com.bcbs239.regtech.ingestion.presentation.batch.suggestion.FileSplittingController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("File splitting suggestion API integration tests")
class FileSplittingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return splitting suggestion for a valid file metadata")
    void shouldReturnSuggestionForValidMetadata() throws Exception {
        var dto = new FileSplittingController.FileMetadataDto(
            "large_file.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            150_000_000L,
            "md5hash",
            "sha256hash"
        );

        String body = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/api/v1/ingestion/suggestions/splitting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fileName").value("large_file.xlsx"))
            .andExpect(jsonPath("$.data.fileSizeBytes").value(150000000))
            .andExpect(jsonPath("$.data.estimatedOptimalFileCount").isNumber());
    }

    @Test
    @DisplayName("Should return validation error for missing fileName")
    void shouldReturnValidationErrorForMissingFileName() throws Exception {
        // fileName is blank
        var map = new java.util.HashMap<String, Object>();
        map.put("fileName", "");
        map.put("contentType", "application/json");
        map.put("fileSizeBytes", 1000);
        map.put("md5Checksum", "md5");
        map.put("sha256Checksum", "sha256");

        String body = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/v1/ingestion/suggestions/splitting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").value("fileName"));
    }
}


