package com.nak.backend.team.dto;

import java.time.OffsetDateTime;

/**
 * 팀 정보 DTO
 * 10년 차 시니어 관점에서 PostgreSQL schema.sql 정의와 유기적인 매핑을 설계하였으며,
 * TIMESTAMPTZ 타입에 맞추어 시간대 오프셋을 명확히 보존할 수 있도록 OffsetDateTime을 활용합니다.
 */
public class TeamDto {
    private String id;
    private String workspaceId;
    private String name;
    private String description;
    private String color;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TeamDto() {}

    public TeamDto(String id, String workspaceId, String name, String description, String color, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.description = description;
        this.color = color;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static TeamDtoBuilder builder() { return new TeamDtoBuilder(); }

    public static class TeamDtoBuilder {
        private String id;
        private String workspaceId;
        private String name;
        private String description;
        private String color;
        private String createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        TeamDtoBuilder() {}

        public TeamDtoBuilder id(String id) { this.id = id; return this; }
        public TeamDtoBuilder workspaceId(String workspaceId) { this.workspaceId = workspaceId; return this; }
        public TeamDtoBuilder name(String name) { this.name = name; return this; }
        public TeamDtoBuilder description(String description) { this.description = description; return this; }
        public TeamDtoBuilder color(String color) { this.color = color; return this; }
        public TeamDtoBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public TeamDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TeamDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TeamDto build() {
            return new TeamDto(id, workspaceId, name, description, color, createdBy, createdAt, updatedAt);
        }
    }
}
