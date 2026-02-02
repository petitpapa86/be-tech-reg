package com.bcbs239.regtech.metrics.domain;

public enum ReportStatus {
    COMPLETED("Completato"),
    SENT("Inviato"),
    PARTIAL("Parziale"),
    ARCHIVED("Archiviato");

    private final String italianTranslation;

    ReportStatus(String italianTranslation) {
        this.italianTranslation = italianTranslation;
    }

    public String getItalianTranslation() {
        return italianTranslation;
    }

    public static ReportStatus fromString(String value) {
        if (value == null) return null;
        for (ReportStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.italianTranslation.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}