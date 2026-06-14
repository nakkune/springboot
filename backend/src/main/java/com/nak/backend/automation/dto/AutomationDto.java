package com.nak.backend.automation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AutomationDto {
    private String id;
    private String boardId;
    private String name;
    private Boolean isActive;
    
    // JSON 형식을 String으로 보관 (MyBatis TypeHandler 또는 형변환을 통해 JSONB 연동)
    private String triggerConfig;
    private String conditionConfig;
    private String actionConfig;
    
    private Integer runCount;
    private LocalDateTime lastRunAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
