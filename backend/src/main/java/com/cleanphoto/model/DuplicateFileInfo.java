package com.cleanphoto.model;

import lombok.Data;
import java.util.List;

@Data
public class DuplicateFileInfo {
    private String fileHash;
    private String fileName;
    private List<String> sourcePaths;
    private String targetPath;
    private DuplicateAction action;
    
    public enum DuplicateAction {
        SKIP,
        OVERWRITE
    }
}
