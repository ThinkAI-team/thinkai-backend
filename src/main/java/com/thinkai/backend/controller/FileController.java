package com.thinkai.backend.controller;

import com.thinkai.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "text/plain",
            "text/markdown",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf", ".txt", ".md", ".doc", ".docx");

    @Value("${app.backend-url:http://localhost:8081}")
    private String backendUrl;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            String safeFilename = Paths.get(filename).getFileName().toString();
            Path filePath = UPLOAD_DIR.resolve(safeFilename).normalize();

            if (!filePath.startsWith(UPLOAD_DIR)) {
                throw new ApiException("Invalid file path", HttpStatus.BAD_REQUEST);
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new ApiException("File not found", HttpStatus.NOT_FOUND);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("File not found", HttpStatus.NOT_FOUND);
            }

            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFilename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ApiException("Invalid file URL", HttpStatus.BAD_REQUEST);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unable to read file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ApiException("File is required", HttpStatus.BAD_REQUEST);
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ApiException("File too large (max 10MB)", HttpStatus.BAD_REQUEST);
            }

            if (!isAllowedFile(file)) {
                throw new ApiException("Unsupported file type", HttpStatus.BAD_REQUEST);
            }

            Files.createDirectories(UPLOAD_DIR);

            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot);
            }
            String safeName = UUID.randomUUID() + ext;
            Path destination = UPLOAD_DIR.resolve(safeName).normalize();

            if (!destination.startsWith(UPLOAD_DIR)) {
                throw new ApiException("Invalid destination path", HttpStatus.BAD_REQUEST);
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String root = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
            String url = root + "/api/files/" + safeName;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unable to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isAllowedFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_TYPES.contains(contentType)) {
            return true;
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return false;
        }
        String lower = original.toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
