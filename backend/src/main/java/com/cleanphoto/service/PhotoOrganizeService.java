package com.cleanphoto.service;

import com.cleanphoto.model.*;
import com.cleanphoto.util.FileHashUtil;
import com.cleanphoto.util.PhotoMetadataExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhotoOrganizeService {

    @Autowired
    private PhotoMetadataExtractor metadataExtractor;
    
    @Autowired
    private FileHashUtil fileHashUtil;
    
    @Autowired
    private GeoLocationService geoLocationService;

    private final Map<String, String> processedFiles = new ConcurrentHashMap<>();

    public List<PhotoInfo> scanSourceFolders(List<String> sourceFolders) {
        List<PhotoInfo> photos = new ArrayList<>();
        
        for (String folder : sourceFolders) {
            File sourceDir = new File(folder);
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                log.warn("Source folder does not exist: {}", folder);
                continue;
            }
            scanFolder(sourceDir, photos);
        }
        
        return photos;
    }

    private void scanFolder(File folder, List<PhotoInfo> photos) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolder(file, photos);
            } else if (fileHashUtil.isPhotoFile(file)) {
                PhotoInfo photoInfo = metadataExtractor.extractPhotoInfo(file);
                if (photoInfo != null) {
                    String fileHash = fileHashUtil.calculateFileHash(file);
                    photoInfo.setFileHash(fileHash);
                    geoLocationService.resolveGeoLocation(photoInfo);
                    photos.add(photoInfo);
                }
            }
        }
    }

    public List<DuplicateFileInfo> detectDuplicates(List<PhotoInfo> photos, String targetFolder) {
        Map<String, List<PhotoInfo>> hashGroupMap = new HashMap<>();
        List<DuplicateFileInfo> duplicates = new ArrayList<>();

        for (PhotoInfo photo : photos) {
            if (photo.getFileHash() != null) {
                hashGroupMap.computeIfAbsent(photo.getFileHash(), k -> new ArrayList<>())
                           .add(photo);
            }
        }

        for (Map.Entry<String, List<PhotoInfo>> entry : hashGroupMap.entrySet()) {
            if (entry.getValue().size() > 1 || targetFolderExists(entry.getKey(), targetFolder)) {
                DuplicateFileInfo info = new DuplicateFileInfo();
                info.setFileHash(entry.getKey());
                info.setFileName(entry.getValue().get(0).getFileName());
                info.setSourcePaths(entry.getValue().stream()
                        .map(PhotoInfo::getOriginalPath)
                        .collect(Collectors.toList()));
                info.setAction(DuplicateFileInfo.DuplicateAction.SKIP);
                duplicates.add(info);
            }
        }

        return duplicates;
    }

    private boolean targetFolderExists(String fileHash, String targetFolder) {
        if (fileHash == null || targetFolder == null) return false;
        return processedFiles.containsKey(fileHash + ":" + targetFolder);
    }

    public OrganizeResult organizePhotos(OrganizeRequest request, List<DuplicateFileInfo> duplicateActions) {
        OrganizeResult result = new OrganizeResult();
        result.setSuccess(true);
        result.setErrorFiles(new ArrayList<>());
        processedFiles.clear();

        try {
            List<PhotoInfo> photos = scanSourceFolders(request.getSourceFolders());
            result.setTotalFiles(photos.size());

            List<DuplicateFileInfo> detectedDuplicates = detectDuplicates(photos, request.getTargetFolder());
            
            Map<String, DuplicateFileInfo> actionMap = new HashMap<>();
            for (DuplicateFileInfo action : duplicateActions) {
                actionMap.put(action.getFileHash(), action);
            }

            int processedCount = 0;
            int duplicateCount = 0;

            for (PhotoInfo photo : photos) {
                try {
                    boolean isDuplicate = isDuplicatePhoto(photo, detectedDuplicates);
                    if (isDuplicate) {
                        DuplicateFileInfo action = actionMap.get(photo.getFileHash());
                        if (action == null || action.getAction() == DuplicateFileInfo.DuplicateAction.SKIP) {
                            duplicateCount++;
                            continue;
                        }
                    }

                    String targetPath = buildTargetPath(photo, request);
                    copyPhoto(photo, targetPath);
                    processedFiles.put(photo.getFileHash() + ":" + request.getTargetFolder(), targetPath);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Failed to process photo: {}", photo.getOriginalPath(), e);
                    result.getErrorFiles().add(photo.getOriginalPath());
                    result.setSuccess(false);
                }
            }

            result.setProcessedFiles(processedCount);
            result.setDuplicateFiles(duplicateCount);
            result.setMessage("照片整理完成");
            result.setDuplicates(duplicateActions);

        } catch (Exception e) {
            log.error("Organize photos failed", e);
            result.setSuccess(false);
            result.setMessage("整理失败: " + e.getMessage());
        }

        return result;
    }

    private boolean isDuplicatePhoto(PhotoInfo photo, List<DuplicateFileInfo> detectedDuplicates) {
        return detectedDuplicates.stream()
                .anyMatch(d -> d.getFileHash().equals(photo.getFileHash()));
    }

    private String buildTargetPath(PhotoInfo photo, OrganizeRequest request) {
        String basePath = request.getTargetFolder();
        
        switch (request.getMode()) {
            case BY_TIME:
                return buildTimeBasedPath(basePath, photo, request.getTimeUnit());
            case BY_LOCATION:
                return buildLocationBasedPath(basePath, photo);
            case BY_TIME_AND_LOCATION:
                return buildTimeAndLocationBasedPath(basePath, photo, request.getTimeUnit());
            default:
                return buildTimeBasedPath(basePath, photo, request.getTimeUnit());
        }
    }

    private String buildTimeBasedPath(String basePath, PhotoInfo photo, OrganizeRequest.TimeUnit timeUnit) {
        LocalDateTime dateTime = getEffectiveDateTime(photo);
        String year = dateTime.format(DateTimeFormatter.ofPattern("yyyy"));
        
        StringBuilder pathBuilder = new StringBuilder(basePath);
        pathBuilder.append(File.separator).append(year);

        if (timeUnit == OrganizeRequest.TimeUnit.MONTH || timeUnit == OrganizeRequest.TimeUnit.DAY) {
            String month = dateTime.format(DateTimeFormatter.ofPattern("MM"));
            pathBuilder.append(File.separator).append(month);
        }

        if (timeUnit == OrganizeRequest.TimeUnit.DAY) {
            String day = dateTime.format(DateTimeFormatter.ofPattern("dd"));
            pathBuilder.append(File.separator).append(day);
        }

        pathBuilder.append(File.separator).append(photo.getFileName());
        return pathBuilder.toString();
    }

    private String buildLocationBasedPath(String basePath, PhotoInfo photo) {
        StringBuilder pathBuilder = new StringBuilder(basePath);
        
        String country = photo.getCountry();
        String province = photo.getProvince();
        String city = photo.getCity();

        if (country == null || country.isEmpty() || "未知国家".equals(country)) {
            pathBuilder.append(File.separator).append("未知位置");
        } else {
            pathBuilder.append(File.separator).append(country);
            
            if (province != null && !province.isEmpty() && !"未知省市".equals(province)) {
                pathBuilder.append(File.separator).append(province);
                
                if (city != null && !city.isEmpty() && !"未知城市".equals(city)) {
                    pathBuilder.append(File.separator).append(city);
                }
            }
        }

        pathBuilder.append(File.separator).append(photo.getFileName());
        return pathBuilder.toString();
    }

    private String buildTimeAndLocationBasedPath(String basePath, PhotoInfo photo, OrganizeRequest.TimeUnit timeUnit) {
        LocalDateTime dateTime = getEffectiveDateTime(photo);
        String year = dateTime.format(DateTimeFormatter.ofPattern("yyyy"));
        
        String country = photo.getCountry();
        String province = photo.getProvince();
        String city = photo.getCity();

        StringBuilder pathBuilder = new StringBuilder(basePath);
        
        if (country == null || country.isEmpty() || "未知国家".equals(country)) {
            pathBuilder.append(File.separator).append(year).append("-未知国家");
        } else {
            pathBuilder.append(File.separator).append(year).append("-").append(country);
        }

        if (timeUnit == OrganizeRequest.TimeUnit.MONTH || timeUnit == OrganizeRequest.TimeUnit.DAY) {
            String month = dateTime.format(DateTimeFormatter.ofPattern("MM"));
            
            if (province != null && !province.isEmpty() && !"未知省市".equals(province)) {
                String provinceCity = province;
                if (city != null && !city.isEmpty() && !"未知城市".equals(city)) {
                    provinceCity = provinceCity + city;
                }
                pathBuilder.append(File.separator).append(month).append("-").append(provinceCity);
            } else {
                pathBuilder.append(File.separator).append(month).append("-未知省市");
            }
        }

        if (timeUnit == OrganizeRequest.TimeUnit.DAY) {
            String day = dateTime.format(DateTimeFormatter.ofPattern("dd"));
            pathBuilder.append(File.separator).append(day);
        }

        pathBuilder.append(File.separator).append(photo.getFileName());
        return pathBuilder.toString();
    }

    private LocalDateTime getEffectiveDateTime(PhotoInfo photo) {
        if (photo.getCaptureTime() != null) {
            return photo.getCaptureTime();
        }
        if (photo.getFileCreateTime() != null) {
            return photo.getFileCreateTime();
        }
        if (photo.getFileModifyTime() != null) {
            return photo.getFileModifyTime();
        }
        return LocalDateTime.now();
    }

    private void copyPhoto(PhotoInfo photo, String targetPath) throws IOException {
        File sourceFile = new File(photo.getOriginalPath());
        File targetFile = new File(targetPath);
        
        File targetDir = targetFile.getParentFile();
        if (!targetDir.exists()) {
            Files.createDirectories(targetDir.toPath());
        }

        FileUtils.copyFile(sourceFile, targetFile);
        log.debug("Copied: {} -> {}", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
    }
}
