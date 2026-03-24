package com.thinkai.backend.controller;

import com.thinkai.backend.exception.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

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
}
