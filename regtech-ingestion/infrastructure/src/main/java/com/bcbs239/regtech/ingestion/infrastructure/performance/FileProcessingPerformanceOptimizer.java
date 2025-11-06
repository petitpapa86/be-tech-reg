package com.bcbs239.regtech.ingestion.infrastructure.performance;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Service for optimizing file processing performance through parallel processing
 * and memory-efficient streaming operations.
 */
@Service
public class FileProcessingPerformanceOptimizer {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingPerformanceOptimizer.class);
    private final ExecutorService fileProcessingExecutor;
    private final int maxConcurrentFiles;
    private final int chunkSize;
    private final AtomicInteger activeProcessingCount = new AtomicInteger(0);

    public FileProcessingPerformanceOptimizer(
            @Value("${ingestion.performance.max-concurrent-files:4}") int maxConcurrentFiles,
            @Value("${ingestion.performance.chunk-size:10000}") int chunkSize) {
        this.maxConcurrentFiles = maxConcurrentFiles;
        this.chunkSize = chunkSize;
        this.fileProcessingExecutor = Executors.newFixedThreadPool(
            maxConcurrentFiles,
            r -> {
                Thread t = new Thread(r, "file-processing-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
        
        log.info("Initialized FileProcessingPerformanceOptimizer with {} concurrent files, chunk size: {}", 
            maxConcurrentFiles, chunkSize);
    }

    /**
     * Process multiple files concurrently with resource management.
     */
    public <T> CompletableFuture<List<Result<T>>> processFilesConcurrently(
            List<FileProcessingTask<T>> tasks) {
        
        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        log.info("Starting concurrent processing of {} files", tasks.size());
        
        List<CompletableFuture<Result<T>>> futures = new ArrayList<>();
        
        for (FileProcessingTask<T> task : tasks) {
            CompletableFuture<Result<T>> future = CompletableFuture
                .supplyAsync(() -> {
                    int currentCount = activeProcessingCount.incrementAndGet();
                    log.debug("Starting file processing task: {} (active: {})", 
                        task.getFileName(), currentCount);
                    
                    try {
                        return task.getProcessor().apply(task);
                    } catch (Exception e) {
                        log.error("Error processing file: {}", task.getFileName(), e);
                        return Result.<T>failure(ErrorDetail.of("PROCESSING_ERROR", 
                            "Failed to process file: " + e.getMessage()));
                    } finally {
                        int remaining = activeProcessingCount.decrementAndGet();
                        log.debug("Completed file processing task: {} (active: {})", 
                            task.getFileName(), remaining);
                    }
                }, fileProcessingExecutor)
                .orTimeout(30, TimeUnit.MINUTES); // Timeout for large files
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Check if system can handle additional concurrent processing.
     */
    public boolean canAcceptMoreFiles() {
        int current = activeProcessingCount.get();
        boolean canAccept = current < maxConcurrentFiles;
        
        if (!canAccept) {
            log.warn("Maximum concurrent file processing limit reached: {}/{}", 
                current, maxConcurrentFiles);
        }
        
        return canAccept;
    }

    /**
     * Get current processing statistics.
     */
    public ProcessingStats getProcessingStats() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) fileProcessingExecutor;
        
        return ProcessingStats.builder()
            .activeProcessingCount(activeProcessingCount.get())
            .maxConcurrentFiles(maxConcurrentFiles)
            .chunkSize(chunkSize)
            .queueSize(executor.getQueue().size())
            .completedTaskCount(executor.getCompletedTaskCount())
            .totalTaskCount(executor.getTaskCount())
            .build();
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public void shutdown() {
        log.info("Shutting down file processing executor");
        fileProcessingExecutor.shutdown();
        
        try {
            if (!fileProcessingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate gracefully, forcing shutdown");
                fileProcessingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for executor shutdown");
            fileProcessingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Task definition for concurrent file processing.
     */
    public static class FileProcessingTask<T> {
        private final String fileName;
        private final Function<FileProcessingTask<T>, Result<T>> processor;
        private final Object data;

        public FileProcessingTask(String fileName, Object data, 
                                Function<FileProcessingTask<T>, Result<T>> processor) {
            this.fileName = fileName;
            this.data = data;
            this.processor = processor;
        }

        public String getFileName() { return fileName; }
        public Object getData() { return data; }
        public Function<FileProcessingTask<T>, Result<T>> getProcessor() { return processor; }
    }

    /**
     * Processing statistics for monitoring.
     */
    public static class ProcessingStats {
        private final int activeProcessingCount;
        private final int maxConcurrentFiles;
        private final int chunkSize;
        private final int queueSize;
        private final long completedTaskCount;
        private final long totalTaskCount;

        private ProcessingStats(Builder builder) {
            this.activeProcessingCount = builder.activeProcessingCount;
            this.maxConcurrentFiles = builder.maxConcurrentFiles;
            this.chunkSize = builder.chunkSize;
            this.queueSize = builder.queueSize;
            this.completedTaskCount = builder.completedTaskCount;
            this.totalTaskCount = builder.totalTaskCount;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public int getActiveProcessingCount() { return activeProcessingCount; }
        public int getMaxConcurrentFiles() { return maxConcurrentFiles; }
        public int getChunkSize() { return chunkSize; }
        public int getQueueSize() { return queueSize; }
        public long getCompletedTaskCount() { return completedTaskCount; }
        public long getTotalTaskCount() { return totalTaskCount; }

        public static class Builder {
            private int activeProcessingCount;
            private int maxConcurrentFiles;
            private int chunkSize;
            private int queueSize;
            private long completedTaskCount;
            private long totalTaskCount;

            public Builder activeProcessingCount(int activeProcessingCount) {
                this.activeProcessingCount = activeProcessingCount;
                return this;
            }

            public Builder maxConcurrentFiles(int maxConcurrentFiles) {
                this.maxConcurrentFiles = maxConcurrentFiles;
                return this;
            }

            public Builder chunkSize(int chunkSize) {
                this.chunkSize = chunkSize;
                return this;
            }

            public Builder queueSize(int queueSize) {
                this.queueSize = queueSize;
                return this;
            }

            public Builder completedTaskCount(long completedTaskCount) {
                this.completedTaskCount = completedTaskCount;
                return this;
            }

            public Builder totalTaskCount(long totalTaskCount) {
                this.totalTaskCount = totalTaskCount;
                return this;
            }

            public ProcessingStats build() {
                return new ProcessingStats(this);
            }
        }
    }

    /**
     * Produce a suggestion whether the provided file should be split for processing.
     * The heuristic is simple and conservative: if the file size exceeds the domain's
     * maximum allowed file size the splitting is required. If the estimated exposure
     * count (if provided) exceeds the configured chunkSize the splitting is recommended.
     *
     * This method returns a domain-level `FileSplittingSuggestion` so callers in the
     * application layer can present user-friendly guidance.
     */
    public FileSplittingSuggestion suggestSplitting(FileMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("FileMetadata cannot be null");
        }

        long fileSize = metadata.fileSizeBytes();
        Integer estimatedExposures = null; // we don't have exposures in FileMetadata; caller may enrich

        // required if exceeds hard max
        boolean required = fileSize > FileMetadata.getMaxFileSizeBytes();

        // recommended if estimated exposures are larger than chunk size
        boolean recommended = false;
        if (estimatedExposures != null) {
            recommended = estimatedExposures > this.chunkSize;
        }

        // If exposures unknown, use a heuristic by bytes: recommend if file > 100MB and chunkSize small
        if (estimatedExposures == null) {
            recommended = recommended || (fileSize > 100L * 1024 * 1024 && this.chunkSize < 100_000);
        }

        String severity = required ? "CRITICAL" : (recommended ? "WARNING" : "INFO");
        String reason = required ? "File exceeds maximum allowed size" : (recommended ? "Large file may impair processing throughput" : "File size within acceptable limits");
        String recommendation = required ? "Split file into multiple files before upload" : (recommended ? "Consider splitting file into smaller chunks or reduce chunk size" : "No action required");

        int estimatedOptimalFiles = 1;
        if (estimatedExposures != null && estimatedExposures > 0) {
            estimatedOptimalFiles = Math.max(1, (int) Math.ceil((double) estimatedExposures / this.chunkSize));
        } else {
            // heuristic: assume avg 1KB per exposure
            long approxRecords = Math.max(1L, fileSize / 1024L);
            estimatedOptimalFiles = Math.max(1, (int) Math.ceil((double) approxRecords / this.chunkSize));
        }

        FileSplittingSuggestion suggestion = new FileSplittingSuggestion(
            metadata.fileName(),
            metadata.fileSizeBytes(),
            estimatedExposures,
            required,
            recommended,
            severity,
            reason,
            recommendation,
            estimatedOptimalFiles
        );

        log.info("Generated splitting suggestion for {}: required={}, recommended={}, optimalFiles={}", metadata.fileName(), required, recommended, estimatedOptimalFiles);

        return suggestion;
    }
}



