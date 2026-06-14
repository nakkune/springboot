package com.nak.backend.item.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDto {
    private String id;
    private String itemId;
    private String parentId;
    private String authorId;
    private String body;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 조인을 위한 추가 필드
    private String authorName;
    private String authorAvatarUrl;
    
    // 프론트엔드에서 전송할 멘션된 유저 ID 목록
    private List<String> mentionedUserIds;
}
