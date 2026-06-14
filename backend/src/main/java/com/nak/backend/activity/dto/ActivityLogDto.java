package com.nak.backend.activity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityLogDto {
    private String id;
    private String workspaceId;
    private String projectId;
    private String boardId;
    private String itemId;
    private String actorId;
    private String action; // 'item.create', 'item.update', 'status.change', etc.
    private String meta; // JSON string
    private LocalDateTime createdAt;
    
    // Additional fields for frontend display
    private String actorName;
    private String actorAvatar;
}
