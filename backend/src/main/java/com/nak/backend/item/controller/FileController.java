package com.nak.backend.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Tag(name = "파일", description = "파일 업로드 관리 API")
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    public FileController() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Operation(summary = "파일 업로드", description = "단일 파일을 로컬 스토리지에 업로드하고 파일 정보를 반환합니다.")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        }

        try {
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFilename = UUID.randomUUID().toString() + extension;
            
            Path targetLocation = Paths.get(UPLOAD_DIR).resolve(savedFilename);
            Files.copy(file.getInputStream(), targetLocation);
            
            // 실제 서비스에서는 S3/GCS 등의 URL을 반환해야 함. 임시로 로컬 경로 반환.
            String fileUrl = "/uploads/" + savedFilename;
            
            return ResponseEntity.ok(Map.of(
                    "originalName", originalFilename,
                    "url", fileUrl,
                    "mimeType", file.getContentType(),
                    "size", String.valueOf(file.getSize())
            ));
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "파일 업로드 실패"));
        }
    }
}
