package com.nak.backend.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private String id;
    private String recipientId;
    private String senderId;
    private String type; // 'mention', 'assigned', 'due_soon', 'status_change', 'automation'
    private String title;
    private String body;
    private String refType; // 'item', 'comment', 'board'
    private String refId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
