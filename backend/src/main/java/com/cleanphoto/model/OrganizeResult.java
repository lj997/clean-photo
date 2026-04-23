package com.cleanphoto.model;

import lombok.Data;
import java.util.List;

@Data
public class OrganizeResult {
    private boolean success;
    private String message;
    private int totalFiles;
    private int processedFiles;
    private int duplicateFiles;
    private List<DuplicateFileInfo> duplicates;
    private List<String> errorFiles;
}
