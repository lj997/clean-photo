package com.cleanphoto.controller;

import com.cleanphoto.model.*;
import com.cleanphoto.service.PhotoOrganizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "http://localhost:3000")
public class PhotoOrganizeController {

    @Autowired
    private PhotoOrganizeService photoOrganizeService;

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanPhotos(@RequestBody Map<String, List<String>> request) {
        List<String> sourceFolders = request.get("sourceFolders");
        Map<String, Object> response = new HashMap<>();
        
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择源文件夹");
            return ResponseEntity.badRequest().body(response);
        }

        for (String folder : sourceFolders) {
            File dir = new File(folder);
            if (!dir.exists() || !dir.isDirectory()) {
                response.put("success", false);
                response.put("message", "文件夹不存在: " + folder);
                return ResponseEntity.badRequest().body(response);
            }
        }

        List<PhotoInfo> photos = photoOrganizeService.scanSourceFolders(sourceFolders);
        
        response.put("success", true);
        response.put("totalPhotos", photos.size());
        response.put("photos", photos);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/detect-duplicates")
    public ResponseEntity<Map<String, Object>> detectDuplicates(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> sourceFolders = (List<String>) request.get("sourceFolders");
        String targetFolder = (String) request.get("targetFolder");
        
        Map<String, Object> response = new HashMap<>();
        
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择源文件夹");
            return ResponseEntity.badRequest().body(response);
        }

        for (String folder : sourceFolders) {
            File dir = new File(folder);
            if (!dir.exists() || !dir.isDirectory()) {
                response.put("success", false);
                response.put("message", "文件夹不存在: " + folder);
                return ResponseEntity.badRequest().body(response);
            }
        }

        List<PhotoInfo> photos = photoOrganizeService.scanSourceFolders(sourceFolders);
        List<DuplicateFileInfo> duplicates = photoOrganizeService.detectDuplicates(photos, targetFolder);

        response.put("success", true);
        response.put("totalPhotos", photos.size());
        response.put("duplicateGroups", duplicates.size());
        response.put("duplicates", duplicates);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organize")
    public ResponseEntity<OrganizeResult> organizePhotos(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> sourceFolders = (List<String>) request.get("sourceFolders");
        String targetFolder = (String) request.get("targetFolder");
        String modeStr = (String) request.get("mode");
        String timeUnitStr = (String) request.get("timeUnit");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> duplicateActionsList = (List<Map<String, Object>>) request.get("duplicateActions");

        OrganizeRequest organizeRequest = new OrganizeRequest();
        organizeRequest.setSourceFolders(sourceFolders);
        organizeRequest.setTargetFolder(targetFolder);
        organizeRequest.setMode(OrganizeRequest.OrganizeMode.valueOf(modeStr));
        organizeRequest.setTimeUnit(timeUnitStr != null ? OrganizeRequest.TimeUnit.valueOf(timeUnitStr) : OrganizeRequest.TimeUnit.YEAR);

        List<DuplicateFileInfo> duplicateActions = new ArrayList<>();
        if (duplicateActionsList != null) {
            for (Map<String, Object> actionMap : duplicateActionsList) {
                DuplicateFileInfo info = new DuplicateFileInfo();
                info.setFileHash((String) actionMap.get("fileHash"));
                info.setAction(DuplicateFileInfo.DuplicateAction.valueOf((String) actionMap.get("action")));
                duplicateActions.add(info);
            }
        }

        File targetDir = new File(targetFolder);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        OrganizeResult result = photoOrganizeService.organizePhotos(organizeRequest, duplicateActions);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/drives")
    public ResponseEntity<List<String>> getDrives() {
        List<String> drives = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                drives.add(root.getAbsolutePath());
            }
        }
        return ResponseEntity.ok(drives);
    }

    @PostMapping("/folders")
    public ResponseEntity<List<String>> getFolders(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        List<String> folders = new ArrayList<>();
        
        if (path == null || path.isEmpty()) {
            return ResponseEntity.ok(folders);
        }

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return ResponseEntity.ok(folders);
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.isHidden()) {
                    folders.add(file.getAbsolutePath());
                }
            }
        }

        return ResponseEntity.ok(folders);
    }
}
