package com.bcbs239.regtech.ingestion.domain.file;

import java.io.InputStream;
import java.util.Objects;

/**
 * Domain value object representing file content to be processed.
 * Encapsulates the file stream, name, and format detected by the adapter layer.
 * 
 * This is immutable and thread-safe (the InputStream itself is managed by caller).
 */
public final class FileContent {
    
    private final InputStream stream;
    private final FileName fileName;
    private final FileFormat format;
    
    private FileContent(InputStream stream, FileName fileName, FileFormat format) {
        this.stream = Objects.requireNonNull(stream, "Stream cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.format = Objects.requireNonNull(format, "File format cannot be null");
    }
    
    /**
     * Create FileContent with all required components.
     * The adapter layer is responsible for detecting the format.
     */
    public static FileContent of(InputStream stream, FileName fileName, FileFormat format) {
        return new FileContent(stream, fileName, format);
    }
    
    /**
     * Create FileContent with format auto-detected from content type.
     */
    public static FileContent of(InputStream stream, String fileName, String contentType) {
        return new FileContent(
            stream,
            FileName.create(fileName).getValue().orElseThrow(() -> 
                new IllegalArgumentException("Invalid file name: " + fileName)),
            FileFormat.fromContentType(contentType)
        );
    }
    
    public InputStream getStream() {
        return stream;
    }
    
    public FileName getFileName() {
        return fileName;
    }
    
    public FileFormat getFormat() {
        return format;
    }
    
    /**
     * Check if the file format is supported for ingestion.
     */
    public boolean isSupportedFormat() {
        return format.isSupported();
    }
    
    @Override
    public String toString() {
        return "FileContent{" +
                "fileName=" + fileName +
                ", format=" + format +
                '}';
    }
}
