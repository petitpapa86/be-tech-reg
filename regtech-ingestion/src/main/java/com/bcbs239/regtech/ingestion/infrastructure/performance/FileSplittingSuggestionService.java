package com.bcbs239.regtech.ingestion.infrastructure.performance;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for providing file splitting suggestions to optimize processing performance.
 * Analyzes file characteristics and provides recommendations for optimal file sizes.
 */
@Service
@Slf4j
public class FileSplittingSuggestionService {

    // File size thresholds in bytes
    @Value("${ingestion.file-splitting.optimal-size-mb:50}")
    private int optimalFileSizeMb;
    
    @Value("${ingestion.file-splitting.max-size-mb:500}")
    private int maxFileSizeMb;
    
    @Value("${ingestion.file-splitting.warning-size-mb:400}")
    private int warningSizeMb;
    
    @Value("${ingestion.file-splitting.optimal-exposures-per-file:50000}")
    private int optimalExposuresPerFile;
    
    @Value("${ingestion.file-splitting.max-exposures-per-file:1000000}")
    private int maxExposuresPerFile;

    /**
     * Analyze file and provide splitting suggestions if needed.
     */
    public Result<FileSplittingSuggestion> analyzefile(String fileName, long fileSizeBytes, 
                                                      Integer estimatedExposureCount, String contentType) {
        
        log.debug("Analyzing file for splitting suggestions: {} (size: {}MB, exposures: {})", 
            fileName, fileSizeBytes / 1024 / 1024, estimatedExposureCount);
        
        try {
            FileSplittingSuggestion.Builder suggestionBuilder = FileSplittingSuggestion.builder()
                .fileName(fileName)
                .fileSizeBytes(fileSizeBytes)
                .fileSizeMB(fileSizeBytes / 1024.0 / 1024.0)
                .estimatedExposureCount(estimatedExposureCount)
                .contentType(contentType);
            
            // Check if file exceeds maximum size limit
            if (fileSizeBytes > maxFileSizeMb * 1024L * 1024L) {
                return createSplittingRequiredSuggestion(suggestionBuilder, fileSizeBytes, estimatedExposureCount);
            }
            
            // Check if file approaches warning threshold
            if (fileSizeBytes > warningSizeMb * 1024L * 1024L) {
                return createSplittingRecommendedSuggestion(suggestionBuilder, fileSizeBytes, estimatedExposureCount);
            }
            
            // Check exposure count limits
            if (estimatedExposureCount != null && estimatedExposureCount > maxExposuresPerFile) {
                return createExposureCountSplittingSuggestion(suggestionBuilder, estimatedExposureCount);
            }
            
            // File is within acceptable limits
            return createNoSplittingNeededSuggestion(suggestionBuilder, fileSizeBytes, estimatedExposureCount);
            
        } catch (Exception e) {
            log.error("Error analyzing file for splitting suggestions: {}", fileName, e);
            return Result.failure(ErrorDetail.of("ANALYSIS_ERROR", 
                "Failed to analyze file for splitting suggestions: " + e.getMessage()));
        }
    }

    /**
     * Generate optimal splitting strategy based on file characteristics.
     */
    public Result<SplittingStrategy> generateSplittingStrategy(String fileName, long fileSizeBytes, 
                                                             int exposureCount, String contentType) {
        
        log.info("Generating splitting strategy for file: {} (size: {}MB, exposures: {})", 
            fileName, fileSizeBytes / 1024 / 1024, exposureCount);
        
        try {
            SplittingStrategy.Builder strategyBuilder = SplittingStrategy.builder()
                .originalFileName(fileName)
                .originalFileSizeBytes(fileSizeBytes)
                .originalExposureCount(exposureCount)
                .contentType(contentType);
            
            // Calculate optimal number of files based on size and exposure count
            int filesBySize = calculateOptimalFilesBySize(fileSizeBytes);
            int filesByExposures = calculateOptimalFilesByExposures(exposureCount);
            
            // Use the higher number to ensure both constraints are met
            int recommendedFileCount = Math.max(filesBySize, filesByExposures);
            
            if (recommendedFileCount <= 1) {
                return Result.success(strategyBuilder
                    .recommendedFileCount(1)
                    .splittingRequired(false)
                    .reason("File is within optimal size and exposure count limits")
                    .build());
            }
            
            // Calculate target metrics for split files
            int targetExposuresPerFile = exposureCount / recommendedFileCount;
            long targetFileSizeBytes = fileSizeBytes / recommendedFileCount;
            
            List<SplitFileSpec> splitSpecs = generateSplitFileSpecs(
                fileName, recommendedFileCount, targetExposuresPerFile, targetFileSizeBytes);
            
            return Result.success(strategyBuilder
                .recommendedFileCount(recommendedFileCount)
                .targetExposuresPerFile(targetExposuresPerFile)
                .targetFileSizeBytes(targetFileSizeBytes)
                .targetFileSizeMB(targetFileSizeBytes / 1024.0 / 1024.0)
                .splittingRequired(true)
                .reason(String.format("File exceeds optimal limits. Recommended to split into %d files", 
                    recommendedFileCount))
                .splitFileSpecs(splitSpecs)
                .estimatedProcessingTimeReduction(calculateProcessingTimeReduction(recommendedFileCount))
                .build());
            
        } catch (Exception e) {
            log.error("Error generating splitting strategy for file: {}", fileName, e);
            return Result.failure(ErrorDetail.of("STRATEGY_ERROR", 
                "Failed to generate splitting strategy: " + e.getMessage()));
        }
    }

    /**
     * Provide guidance for manual file splitting.
     */
    public Result<SplittingGuidance> provideSplittingGuidance(String contentType, int exposureCount) {
        
        try {
            SplittingGuidance.Builder guidanceBuilder = SplittingGuidance.builder()
                .contentType(contentType)
                .originalExposureCount(exposureCount);
            
            if ("application/json".equals(contentType)) {
                return Result.success(guidanceBuilder
                    .splittingMethod("JSON_ARRAY_SPLITTING")
                    .instructions(generateJsonSplittingInstructions(exposureCount))
                    .toolRecommendations(List.of("jq", "Python json module", "Node.js fs module"))
                    .build());
            } else if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) {
                return Result.success(guidanceBuilder
                    .splittingMethod("EXCEL_ROW_SPLITTING")
                    .instructions(generateExcelSplittingInstructions(exposureCount))
                    .toolRecommendations(List.of("Microsoft Excel", "Python pandas", "Apache POI"))
                    .build());
            } else {
                return Result.failure(ErrorDetail.of("UNSUPPORTED_TYPE", 
                    "File splitting guidance not available for content type: " + contentType));
            }
            
        } catch (Exception e) {
            log.error("Error providing splitting guidance for content type: {}", contentType, e);
            return Result.failure(ErrorDetail.of("GUIDANCE_ERROR", 
                "Failed to provide splitting guidance: " + e.getMessage()));
        }
    }

    private Result<FileSplittingSuggestion> createSplittingRequiredSuggestion(
            FileSplittingSuggestion.Builder builder, long fileSizeBytes, Integer exposureCount) {
        
        return Result.success(builder
            .splittingRequired(true)
            .severity("CRITICAL")
            .reason(String.format("File size (%.1fMB) exceeds maximum limit (%dMB)", 
                fileSizeBytes / 1024.0 / 1024.0, maxFileSizeMb))
            .recommendation(String.format("Split file into smaller chunks of approximately %dMB each", 
                optimalFileSizeMb))
            .estimatedOptimalFileCount(calculateOptimalFilesBySize(fileSizeBytes))
            .build());
    }

    private Result<FileSplittingSuggestion> createSplittingRecommendedSuggestion(
            FileSplittingSuggestion.Builder builder, long fileSizeBytes, Integer exposureCount) {
        
        return Result.success(builder
            .splittingRequired(false)
            .splittingRecommended(true)
            .severity("WARNING")
            .reason(String.format("File size (%.1fMB) approaches maximum limit (%dMB)", 
                fileSizeBytes / 1024.0 / 1024.0, maxFileSizeMb))
            .recommendation(String.format("Consider splitting file into smaller chunks of approximately %dMB each for optimal performance", 
                optimalFileSizeMb))
            .estimatedOptimalFileCount(calculateOptimalFilesBySize(fileSizeBytes))
            .build());
    }

    private Result<FileSplittingSuggestion> createExposureCountSplittingSuggestion(
            FileSplittingSuggestion.Builder builder, int exposureCount) {
        
        return Result.success(builder
            .splittingRequired(true)
            .severity("CRITICAL")
            .reason(String.format("Exposure count (%d) exceeds maximum limit (%d)", 
                exposureCount, maxExposuresPerFile))
            .recommendation(String.format("Split file into smaller files with approximately %d exposures each", 
                optimalExposuresPerFile))
            .estimatedOptimalFileCount(calculateOptimalFilesByExposures(exposureCount))
            .build());
    }

    private Result<FileSplittingSuggestion> createNoSplittingNeededSuggestion(
            FileSplittingSuggestion.Builder builder, long fileSizeBytes, Integer exposureCount) {
        
        return Result.success(builder
            .splittingRequired(false)
            .splittingRecommended(false)
            .severity("INFO")
            .reason("File is within optimal size and exposure count limits")
            .recommendation("No splitting required. File can be processed efficiently as-is.")
            .estimatedOptimalFileCount(1)
            .build());
    }

    private int calculateOptimalFilesBySize(long fileSizeBytes) {
        long optimalSizeBytes = optimalFileSizeMb * 1024L * 1024L;
        return (int) Math.ceil((double) fileSizeBytes / optimalSizeBytes);
    }

    private int calculateOptimalFilesByExposures(int exposureCount) {
        return (int) Math.ceil((double) exposureCount / optimalExposuresPerFile);
    }

    private List<SplitFileSpec> generateSplitFileSpecs(String originalFileName, int fileCount, 
                                                      int targetExposuresPerFile, long targetFileSizeBytes) {
        List<SplitFileSpec> specs = new ArrayList<>();
        String baseName = getFileBaseName(originalFileName);
        String extension = getFileExtension(originalFileName);
        
        for (int i = 1; i <= fileCount; i++) {
            String splitFileName = String.format("%s_part_%02d.%s", baseName, i, extension);
            specs.add(SplitFileSpec.builder()
                .fileName(splitFileName)
                .partNumber(i)
                .targetExposureCount(targetExposuresPerFile)
                .targetFileSizeBytes(targetFileSizeBytes)
                .targetFileSizeMB(targetFileSizeBytes / 1024.0 / 1024.0)
                .build());
        }
        
        return specs;
    }

    private double calculateProcessingTimeReduction(int fileCount) {
        // Estimate processing time reduction based on parallel processing capabilities
        // Assumes diminishing returns with more files
        if (fileCount <= 1) return 0.0;
        if (fileCount <= 4) return 0.3 + (fileCount - 1) * 0.15; // 30-75% reduction
        return 0.75; // Cap at 75% reduction
    }

    private List<String> generateJsonSplittingInstructions(int exposureCount) {
        int recommendedChunkSize = Math.min(optimalExposuresPerFile, exposureCount / 2);
        
        return List.of(
            "1. Parse the JSON array to count total exposures",
            String.format("2. Split the array into chunks of approximately %d exposures each", recommendedChunkSize),
            "3. Create separate JSON files for each chunk",
            "4. Maintain the same JSON structure in each split file",
            "5. Validate each split file contains valid JSON",
            "Example command: jq -c '.[]' input.json | split -l " + recommendedChunkSize + " - output_part_"
        );
    }

    private List<String> generateExcelSplittingInstructions(int exposureCount) {
        int recommendedRowsPerFile = Math.min(optimalExposuresPerFile, exposureCount / 2);
        
        return List.of(
            "1. Open the Excel file and note the total number of data rows",
            String.format("2. Create new workbooks with approximately %d data rows each (plus header)", recommendedRowsPerFile),
            "3. Copy the header row to each new workbook",
            "4. Copy data rows in sequential chunks to each workbook",
            "5. Save each workbook with a descriptive name (e.g., filename_part_01.xlsx)",
            "6. Verify each file opens correctly and contains the expected data"
        );
    }

    private String getFileBaseName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    // Data classes for suggestions and strategies

    public static class FileSplittingSuggestion {
        private final String fileName;
        private final long fileSizeBytes;
        private final double fileSizeMB;
        private final Integer estimatedExposureCount;
        private final String contentType;
        private final boolean splittingRequired;
        private final boolean splittingRecommended;
        private final String severity;
        private final String reason;
        private final String recommendation;
        private final int estimatedOptimalFileCount;

        private FileSplittingSuggestion(Builder builder) {
            this.fileName = builder.fileName;
            this.fileSizeBytes = builder.fileSizeBytes;
            this.fileSizeMB = builder.fileSizeMB;
            this.estimatedExposureCount = builder.estimatedExposureCount;
            this.contentType = builder.contentType;
            this.splittingRequired = builder.splittingRequired;
            this.splittingRecommended = builder.splittingRecommended;
            this.severity = builder.severity;
            this.reason = builder.reason;
            this.recommendation = builder.recommendation;
            this.estimatedOptimalFileCount = builder.estimatedOptimalFileCount;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getFileName() { return fileName; }
        public long getFileSizeBytes() { return fileSizeBytes; }
        public double getFileSizeMB() { return fileSizeMB; }
        public Integer getEstimatedExposureCount() { return estimatedExposureCount; }
        public String getContentType() { return contentType; }
        public boolean isSplittingRequired() { return splittingRequired; }
        public boolean isSplittingRecommended() { return splittingRecommended; }
        public String getSeverity() { return severity; }
        public String getReason() { return reason; }
        public String getRecommendation() { return recommendation; }
        public int getEstimatedOptimalFileCount() { return estimatedOptimalFileCount; }

        public static class Builder {
            private String fileName;
            private long fileSizeBytes;
            private double fileSizeMB;
            private Integer estimatedExposureCount;
            private String contentType;
            private boolean splittingRequired;
            private boolean splittingRecommended;
            private String severity;
            private String reason;
            private String recommendation;
            private int estimatedOptimalFileCount;

            public Builder fileName(String fileName) { this.fileName = fileName; return this; }
            public Builder fileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
            public Builder fileSizeMB(double fileSizeMB) { this.fileSizeMB = fileSizeMB; return this; }
            public Builder estimatedExposureCount(Integer estimatedExposureCount) { this.estimatedExposureCount = estimatedExposureCount; return this; }
            public Builder contentType(String contentType) { this.contentType = contentType; return this; }
            public Builder splittingRequired(boolean splittingRequired) { this.splittingRequired = splittingRequired; return this; }
            public Builder splittingRecommended(boolean splittingRecommended) { this.splittingRecommended = splittingRecommended; return this; }
            public Builder severity(String severity) { this.severity = severity; return this; }
            public Builder reason(String reason) { this.reason = reason; return this; }
            public Builder recommendation(String recommendation) { this.recommendation = recommendation; return this; }
            public Builder estimatedOptimalFileCount(int estimatedOptimalFileCount) { this.estimatedOptimalFileCount = estimatedOptimalFileCount; return this; }

            public FileSplittingSuggestion build() { return new FileSplittingSuggestion(this); }
        }
    }

    public static class SplittingStrategy {
        private final String originalFileName;
        private final long originalFileSizeBytes;
        private final int originalExposureCount;
        private final String contentType;
        private final int recommendedFileCount;
        private final int targetExposuresPerFile;
        private final long targetFileSizeBytes;
        private final double targetFileSizeMB;
        private final boolean splittingRequired;
        private final String reason;
        private final List<SplitFileSpec> splitFileSpecs;
        private final double estimatedProcessingTimeReduction;

        private SplittingStrategy(Builder builder) {
            this.originalFileName = builder.originalFileName;
            this.originalFileSizeBytes = builder.originalFileSizeBytes;
            this.originalExposureCount = builder.originalExposureCount;
            this.contentType = builder.contentType;
            this.recommendedFileCount = builder.recommendedFileCount;
            this.targetExposuresPerFile = builder.targetExposuresPerFile;
            this.targetFileSizeBytes = builder.targetFileSizeBytes;
            this.targetFileSizeMB = builder.targetFileSizeMB;
            this.splittingRequired = builder.splittingRequired;
            this.reason = builder.reason;
            this.splitFileSpecs = builder.splitFileSpecs;
            this.estimatedProcessingTimeReduction = builder.estimatedProcessingTimeReduction;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getOriginalFileName() { return originalFileName; }
        public long getOriginalFileSizeBytes() { return originalFileSizeBytes; }
        public int getOriginalExposureCount() { return originalExposureCount; }
        public String getContentType() { return contentType; }
        public int getRecommendedFileCount() { return recommendedFileCount; }
        public int getTargetExposuresPerFile() { return targetExposuresPerFile; }
        public long getTargetFileSizeBytes() { return targetFileSizeBytes; }
        public double getTargetFileSizeMB() { return targetFileSizeMB; }
        public boolean isSplittingRequired() { return splittingRequired; }
        public String getReason() { return reason; }
        public List<SplitFileSpec> getSplitFileSpecs() { return splitFileSpecs; }
        public double getEstimatedProcessingTimeReduction() { return estimatedProcessingTimeReduction; }

        public static class Builder {
            private String originalFileName;
            private long originalFileSizeBytes;
            private int originalExposureCount;
            private String contentType;
            private int recommendedFileCount;
            private int targetExposuresPerFile;
            private long targetFileSizeBytes;
            private double targetFileSizeMB;
            private boolean splittingRequired;
            private String reason;
            private List<SplitFileSpec> splitFileSpecs;
            private double estimatedProcessingTimeReduction;

            public Builder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
            public Builder originalFileSizeBytes(long originalFileSizeBytes) { this.originalFileSizeBytes = originalFileSizeBytes; return this; }
            public Builder originalExposureCount(int originalExposureCount) { this.originalExposureCount = originalExposureCount; return this; }
            public Builder contentType(String contentType) { this.contentType = contentType; return this; }
            public Builder recommendedFileCount(int recommendedFileCount) { this.recommendedFileCount = recommendedFileCount; return this; }
            public Builder targetExposuresPerFile(int targetExposuresPerFile) { this.targetExposuresPerFile = targetExposuresPerFile; return this; }
            public Builder targetFileSizeBytes(long targetFileSizeBytes) { this.targetFileSizeBytes = targetFileSizeBytes; return this; }
            public Builder targetFileSizeMB(double targetFileSizeMB) { this.targetFileSizeMB = targetFileSizeMB; return this; }
            public Builder splittingRequired(boolean splittingRequired) { this.splittingRequired = splittingRequired; return this; }
            public Builder reason(String reason) { this.reason = reason; return this; }
            public Builder splitFileSpecs(List<SplitFileSpec> splitFileSpecs) { this.splitFileSpecs = splitFileSpecs; return this; }
            public Builder estimatedProcessingTimeReduction(double estimatedProcessingTimeReduction) { this.estimatedProcessingTimeReduction = estimatedProcessingTimeReduction; return this; }

            public SplittingStrategy build() { return new SplittingStrategy(this); }
        }
    }

    public static class SplitFileSpec {
        private final String fileName;
        private final int partNumber;
        private final int targetExposureCount;
        private final long targetFileSizeBytes;
        private final double targetFileSizeMB;

        private SplitFileSpec(Builder builder) {
            this.fileName = builder.fileName;
            this.partNumber = builder.partNumber;
            this.targetExposureCount = builder.targetExposureCount;
            this.targetFileSizeBytes = builder.targetFileSizeBytes;
            this.targetFileSizeMB = builder.targetFileSizeMB;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getFileName() { return fileName; }
        public int getPartNumber() { return partNumber; }
        public int getTargetExposureCount() { return targetExposureCount; }
        public long getTargetFileSizeBytes() { return targetFileSizeBytes; }
        public double getTargetFileSizeMB() { return targetFileSizeMB; }

        public static class Builder {
            private String fileName;
            private int partNumber;
            private int targetExposureCount;
            private long targetFileSizeBytes;
            private double targetFileSizeMB;

            public Builder fileName(String fileName) { this.fileName = fileName; return this; }
            public Builder partNumber(int partNumber) { this.partNumber = partNumber; return this; }
            public Builder targetExposureCount(int targetExposureCount) { this.targetExposureCount = targetExposureCount; return this; }
            public Builder targetFileSizeBytes(long targetFileSizeBytes) { this.targetFileSizeBytes = targetFileSizeBytes; return this; }
            public Builder targetFileSizeMB(double targetFileSizeMB) { this.targetFileSizeMB = targetFileSizeMB; return this; }

            public SplitFileSpec build() { return new SplitFileSpec(this); }
        }
    }

    public static class SplittingGuidance {
        private final String contentType;
        private final int originalExposureCount;
        private final String splittingMethod;
        private final List<String> instructions;
        private final List<String> toolRecommendations;

        private SplittingGuidance(Builder builder) {
            this.contentType = builder.contentType;
            this.originalExposureCount = builder.originalExposureCount;
            this.splittingMethod = builder.splittingMethod;
            this.instructions = builder.instructions;
            this.toolRecommendations = builder.toolRecommendations;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getContentType() { return contentType; }
        public int getOriginalExposureCount() { return originalExposureCount; }
        public String getSplittingMethod() { return splittingMethod; }
        public List<String> getInstructions() { return instructions; }
        public List<String> getToolRecommendations() { return toolRecommendations; }

        public static class Builder {
            private String contentType;
            private int originalExposureCount;
            private String splittingMethod;
            private List<String> instructions;
            private List<String> toolRecommendations;

            public Builder contentType(String contentType) { this.contentType = contentType; return this; }
            public Builder originalExposureCount(int originalExposureCount) { this.originalExposureCount = originalExposureCount; return this; }
            public Builder splittingMethod(String splittingMethod) { this.splittingMethod = splittingMethod; return this; }
            public Builder instructions(List<String> instructions) { this.instructions = instructions; return this; }
            public Builder toolRecommendations(List<String> toolRecommendations) { this.toolRecommendations = toolRecommendations; return this; }

            public SplittingGuidance build() { return new SplittingGuidance(this); }
        }
    }
}