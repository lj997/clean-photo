package com.cleanphoto.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class FileHashUtil {

    public String calculateFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[8192];
                int bytesCount;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to calculate file hash for: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    public boolean isPhotoFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") ||
               fileName.endsWith(".jpeg") ||
               fileName.endsWith(".png") ||
               fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") ||
               fileName.endsWith(".webp") ||
               fileName.endsWith(".tiff") ||
               fileName.endsWith(".tif");
    }
}
