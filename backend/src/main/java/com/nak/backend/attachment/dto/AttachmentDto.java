package com.nak.backend.attachment.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AttachmentDto {
    private String id;
    private String itemId;
    private String commentId;
    private String uploaderId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String storageUrl;
    private LocalDateTime createdAt;
    
    // Additional field for UI
    private String uploaderName;
}
