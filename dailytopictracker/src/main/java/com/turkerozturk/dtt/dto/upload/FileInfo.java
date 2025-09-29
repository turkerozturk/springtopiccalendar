package com.turkerozturk.dtt.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Data;

// FileInfo.java
@Data
public class FileInfo {
    private String name;
    private String relativePath; // relative to baseDir, useful for operations
    private boolean directory;
    private long sizeBytes;
    private String sizeKbFormatted;
    private long lastModified;

    // constructors, getters, setters
}

