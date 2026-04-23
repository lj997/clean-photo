package com.cleanphoto.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PhotoInfo {
    private String originalPath;
    private String fileName;
    private LocalDateTime captureTime;
    private LocalDateTime fileCreateTime;
    private LocalDateTime fileModifyTime;
    private Double latitude;
    private Double longitude;
    private String country;
    private String province;
    private String city;
    private String fileHash;
    private long fileSize;
    private String fileType;
}
