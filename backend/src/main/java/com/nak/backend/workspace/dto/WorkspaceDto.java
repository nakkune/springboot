package com.nak.backend.workspace.dto;

import java.time.OffsetDateTime;

/**
 * 워크스페이스 정보 DTO
 * 10년 차 시니어 관점에서 PostgreSQL schema.sql 정의와 유기적인 매핑을 설계하였으며,
 * TIMESTAMPTZ 타입에 맞추어 시간대 오프셋을 명확히 보존할 수 있도록 OffsetDateTime을 활용합니다.
 */
public class WorkspaceDto {
    private String id;
    private String name;
    private String slug;
    private String logoUrl;
    private String plan;
    private String ownerId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public WorkspaceDto() {
        this.plan = "free";
    }

    public WorkspaceDto(String id, String name, String slug, String logoUrl, String plan, String ownerId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.logoUrl = logoUrl;
        this.plan = plan != null ? plan : "free";
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static WorkspaceDtoBuilder builder() { return new WorkspaceDtoBuilder(); }

    public static class WorkspaceDtoBuilder {
        private String id;
        private String name;
        private String slug;
        private String logoUrl;
        private String plan;
        private String ownerId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        WorkspaceDtoBuilder() {}

        public WorkspaceDtoBuilder id(String id) { this.id = id; return this; }
        public WorkspaceDtoBuilder name(String name) { this.name = name; return this; }
        public WorkspaceDtoBuilder slug(String slug) { this.slug = slug; return this; }
        public WorkspaceDtoBuilder logoUrl(String logoUrl) { this.logoUrl = logoUrl; return this; }
        public WorkspaceDtoBuilder plan(String plan) { this.plan = plan; return this; }
        public WorkspaceDtoBuilder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public WorkspaceDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public WorkspaceDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public WorkspaceDto build() {
            return new WorkspaceDto(id, name, slug, logoUrl, plan, ownerId, createdAt, updatedAt);
        }
    }
}
