package com.nak.backend.project.dto;

import java.time.OffsetDateTime;

/**
 * 프로젝트 정보 DTO
 * 10년 차 시니어 관점에서 PostgreSQL schema.sql 정의와 유기적인 매핑을 설계하였으며,
 * TIMESTAMPTZ 타입에 맞추어 시간대 오프셋을 명확히 보존할 수 있도록 OffsetDateTime을 활용합니다.
 */
public class ProjectDto {
    private String id;
    private String workspaceId;
    private String teamId;
    private String name;
    private String description;
    private String color;
    private String icon;
    private Boolean isArchived;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProjectDto() {
        this.isArchived = false;
    }

    public ProjectDto(String id, String workspaceId, String teamId, String name, String description, String color, String icon, Boolean isArchived, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.teamId = teamId;
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.isArchived = isArchived != null ? isArchived : false;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ProjectDtoBuilder builder() { return new ProjectDtoBuilder(); }

    public static class ProjectDtoBuilder {
        private String id;
        private String workspaceId;
        private String teamId;
        private String name;
        private String description;
        private String color;
        private String icon;
        private Boolean isArchived;
        private String createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        ProjectDtoBuilder() {}

        public ProjectDtoBuilder id(String id) { this.id = id; return this; }
        public ProjectDtoBuilder workspaceId(String workspaceId) { this.workspaceId = workspaceId; return this; }
        public ProjectDtoBuilder teamId(String teamId) { this.teamId = teamId; return this; }
        public ProjectDtoBuilder name(String name) { this.name = name; return this; }
        public ProjectDtoBuilder description(String description) { this.description = description; return this; }
        public ProjectDtoBuilder color(String color) { this.color = color; return this; }
        public ProjectDtoBuilder icon(String icon) { this.icon = icon; return this; }
        public ProjectDtoBuilder isArchived(Boolean isArchived) { this.isArchived = isArchived; return this; }
        public ProjectDtoBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public ProjectDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProjectDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectDto build() {
            return new ProjectDto(id, workspaceId, teamId, name, description, color, icon, isArchived, createdBy, createdAt, updatedAt);
        }
    }
}
