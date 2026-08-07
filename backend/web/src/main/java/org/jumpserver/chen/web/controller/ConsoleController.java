package org.jumpserver.chen.web.controller;

import org.jumpserver.chen.framework.i18n.MessageUtils;
import org.jumpserver.chen.framework.session.SessionManager;
import org.jumpserver.chen.web.entity.UploadResponse;
import org.jumpserver.chen.web.exception.ChenException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/console")
public class ConsoleController {

    @GetMapping("/export/{fileKey}")
    public ResponseEntity<Resource> exportData(@PathVariable String fileKey) {
        if (!SessionManager.getCurrentSession().canDownload()) {
            throw new ChenException(MessageUtils.get("NoPermissionError"));
        }
        // The export filename must be a single filename; reject cross-platform path separators and traversal segments first.
        if (fileKey == null || fileKey.isBlank()
                || fileKey.contains("/") || fileKey.contains("\\") || fileKey.contains("..")) {
            throw new ChenException("Invalid export file");
        }
        var basePath = SessionManager.getCurrentSession().getTempPath().toAbsolutePath().normalize();
        var filePath = basePath.resolve(fileKey).normalize();
        // After normalization it must still fall within the session directory, and the final symlink must not point outside it.
        if (!filePath.startsWith(basePath) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new ChenException("Invalid export file");
        }
        Resource resource = new FileSystemResource(filePath.toFile());
        var resp = ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-disposition", String.format("attachment; filename=%s", fileKey))
                .body(resource);
        return resp;
    }

    @PostMapping("/upload")
    public UploadResponse uploadData(@RequestParam("file") MultipartFile file) {
        if (!SessionManager.getCurrentSession().canUpload()) {
            throw new ChenException(MessageUtils.get("NoPermissionError"));
        }
        try {
            var basePath = SessionManager.getCurrentSession().getTempPath();
            var bytes = file.getBytes();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            var filename = String.format("sql_%s.sql", timestamp);
            var path = basePath.resolve(filename);
            Files.write(path, bytes);
            return new UploadResponse(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
