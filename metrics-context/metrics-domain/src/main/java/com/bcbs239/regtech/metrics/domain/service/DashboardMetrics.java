package com.bcbs239.regtech.metrics.domain.service;

import com.bcbs239.regtech.metrics.domain.model.ComplianceFile;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public class DashboardMetrics {

    public record Result(int filesProcessed, double avgScore, int violations, int reports, int lastBatchViolations) {
    }

    public static Result compute(List<ComplianceFile> files) {
        int filesProcessed = files.size();

        OptionalDouble avg = files.stream()
                .filter(f -> f.getScore() != null)
                .mapToDouble(ComplianceFile::getScore)
                .average();
        double avgScore = avg.orElse(0.0);

        int violations = (int) files.stream().filter(f -> !f.isCompliant()).count();

        int reports = 0; // placeholder, reports not yet modelled

        // compute last batch violations by picking the latest non-null batchId lexicographically
        String latestBatch = files.stream()
                .map(ComplianceFile::getBatchId)
                .filter(Objects::nonNull)
                .max(String::compareTo)
                .orElse(null);

        int lastBatchViolations;
        if (latestBatch != null) {
            final String lb = latestBatch;
            lastBatchViolations = (int) files.stream()
                    .filter(f -> Objects.equals(lb, f.getBatchId()))
                    .filter(f -> !f.isCompliant())
                    .count();
        } else {
            lastBatchViolations = violations;
        }

        return new Result(filesProcessed, avgScore, violations, reports, lastBatchViolations);
    }
}
