package com.cleanphoto.util;

import com.cleanphoto.model.PhotoInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Component
public class PhotoMetadataExtractor {

    private static final DateTimeFormatter EXIF_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public PhotoInfo extractPhotoInfo(File file) {
        PhotoInfo photoInfo = new PhotoInfo();
        photoInfo.setOriginalPath(file.getAbsolutePath());
        photoInfo.setFileName(file.getName());
        photoInfo.setFileSize(file.length());
        photoInfo.setFileType(getFileExtension(file.getName()));

        try {
            extractFileTimes(file, photoInfo);
            extractExifData(file, photoInfo);
        } catch (Exception e) {
            log.warn("Failed to extract metadata from file: {}", file.getAbsolutePath(), e);
        }

        return photoInfo;
    }

    private void extractFileTimes(File file, PhotoInfo photoInfo) {
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            
            photoInfo.setFileCreateTime(toLocalDateTime(attrs.creationTime().toInstant()));
            photoInfo.setFileModifyTime(toLocalDateTime(attrs.lastModifiedTime().toInstant()));
        } catch (IOException e) {
            log.warn("Failed to read file attributes for: {}", file.getAbsolutePath(), e);
        }
    }

    private void extractExifData(File file, PhotoInfo photoInfo) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            
            extractCaptureTime(metadata, photoInfo);
            extractGpsData(metadata, photoInfo);
        } catch (Exception e) {
            log.debug("No EXIF data found in file: {}", file.getAbsolutePath(), e);
        }
    }

    private void extractCaptureTime(Metadata metadata, PhotoInfo photoInfo) {
        ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIfdDirectory != null) {
            Date date = subIfdDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date != null) {
                photoInfo.setCaptureTime(toLocalDateTime(date.toInstant()));
                return;
            }
        }

        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0Directory != null) {
            Date date = ifd0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
            if (date != null) {
                photoInfo.setCaptureTime(toLocalDateTime(date.toInstant()));
            }
        }
    }

    private void extractGpsData(Metadata metadata, PhotoInfo photoInfo) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null && gpsDirectory.getGeoLocation() != null) {
            com.drew.lang.GeoLocation geoLocation = gpsDirectory.getGeoLocation();
            photoInfo.setLatitude(geoLocation.getLatitude());
            photoInfo.setLongitude(geoLocation.getLongitude());
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}
