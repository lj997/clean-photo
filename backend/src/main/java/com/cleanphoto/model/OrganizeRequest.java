package com.cleanphoto.model;

import lombok.Data;
import java.util.List;

@Data
public class OrganizeRequest {
    private List<String> sourceFolders;
    private String targetFolder;
    private OrganizeMode mode;
    private TimeUnit timeUnit;
    
    public enum OrganizeMode {
        BY_TIME,
        BY_LOCATION,
        BY_TIME_AND_LOCATION
    }
    
    public enum TimeUnit {
        YEAR,
        MONTH,
        DAY
    }
}
