package com.nak.backend.attachment.controller;

import com.nak.backend.attachment.dto.AttachmentDto;
import com.nak.backend.attachment.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "첨부파일", description = "첨부파일 관리 API")
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "아이템별 첨부파일 조회", description = "특정 아이템 ID에 해당하는 모든 첨부파일 목록을 조회합니다.")
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsByItemId(@PathVariable String itemId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByItemId(itemId));
    }

    @Operation(summary = "첨부파일 업로드", description = "특정 아이템에 대한 첨부파일을 업로드합니다.")
    @PostMapping("/upload")
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @RequestParam("itemId") String itemId,
            @RequestParam(value = "uploaderId", required = false) String uploaderId,
            @RequestParam("file") MultipartFile file) {
        
        AttachmentDto attachment = attachmentService.uploadAttachment(itemId, uploaderId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    /**
     * [NEW] 아이템 외래키(FK) 제약에 얽매이지 않는 고품격 시니어 설계의 독립형 프로필 사진 업로드 API
     */
    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지 파일을 업로드하고 저장된 URL을 반환합니다.")
    @PostMapping("/upload/profile")
    public ResponseEntity<java.util.Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        try {
            // 업로드 디렉토리 확보
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/");
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // 고유 파일명 생성
            String originalFileName = org.springframework.util.StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar.png"
            );
            String uniqueFileName = java.util.UUID.randomUUID().toString() + "_" + originalFileName;
            java.nio.file.Path filePath = uploadPath.resolve(uniqueFileName);
            
            // 물리 디렉토리에 파일 복사 저장
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // 서블릿 컨텍스트 패스(/api)가 포함된 웹 호출 경로 URL 리턴
            String storageUrl = "/api/uploads/" + uniqueFileName;
            
            return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("url", storageUrl));
        } catch (java.io.IOException e) {
            throw new RuntimeException("프로필 사진 물리적 파일 저장 실패 디버그 에러", e);
        }
    }
}
