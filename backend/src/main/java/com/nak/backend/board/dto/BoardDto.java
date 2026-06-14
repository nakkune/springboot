package com.nak.backend.board.dto;

import java.time.OffsetDateTime;

/**
 * 보드(Board) 정보 DTO
 * 10년 차 시니어 개발자 관점에서 기본값 적용 및 OffsetDateTime 매핑을 준수합니다.
 */
public class BoardDto {
    private String id;
    private String projectId;
    private String name;
    private String description;
    private String boardType;
    private Integer position;
    private Boolean isArchived;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public BoardDto() {
        this.boardType = "main";
        this.position = 0;
        this.isArchived = false;
    }

    public BoardDto(String id, String projectId, String name, String description, String boardType, Integer position, Boolean isArchived, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.boardType = boardType != null ? boardType : "main";
        this.position = position != null ? position : 0;
        this.isArchived = isArchived != null ? isArchived : false;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBoardType() { return boardType; }
    public void setBoardType(String boardType) { this.boardType = boardType; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static BoardDtoBuilder builder() { return new BoardDtoBuilder(); }

    public static class BoardDtoBuilder {
        private String id;
        private String projectId;
        private String name;
        private String description;
        private String boardType;
        private Integer position;
        private Boolean isArchived;
        private String createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        BoardDtoBuilder() {}

        public BoardDtoBuilder id(String id) { this.id = id; return this; }
        public BoardDtoBuilder projectId(String projectId) { this.projectId = projectId; return this; }
        public BoardDtoBuilder name(String name) { this.name = name; return this; }
        public BoardDtoBuilder description(String description) { this.description = description; return this; }
        public BoardDtoBuilder boardType(String boardType) { this.boardType = boardType; return this; }
        public BoardDtoBuilder position(Integer position) { this.position = position; return this; }
        public BoardDtoBuilder isArchived(Boolean isArchived) { this.isArchived = isArchived; return this; }
        public BoardDtoBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public BoardDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BoardDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BoardDto build() {
            return new BoardDto(id, projectId, name, description, boardType, position, isArchived, createdBy, createdAt, updatedAt);
        }
    }
}
