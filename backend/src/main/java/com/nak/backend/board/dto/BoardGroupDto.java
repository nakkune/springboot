package com.nak.backend.board.dto;

import java.time.OffsetDateTime;

/**
 * 보드 그룹 정보 DTO
 */
public class BoardGroupDto {
    private String id;
    private String boardId;
    private String title;
    private String color;
    private Integer position;
    private Boolean isCollapsed;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public BoardGroupDto() {
        this.position = 0;
        this.isCollapsed = false;
    }

    public BoardGroupDto(String id, String boardId, String title, String color, Integer position, Boolean isCollapsed, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.title = title;
        this.color = color;
        this.position = position != null ? position : 0;
        this.isCollapsed = isCollapsed != null ? isCollapsed : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Boolean getIsCollapsed() { return isCollapsed; }
    public void setIsCollapsed(Boolean isCollapsed) { this.isCollapsed = isCollapsed; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static BoardGroupDtoBuilder builder() { return new BoardGroupDtoBuilder(); }

    public static class BoardGroupDtoBuilder {
        private String id;
        private String boardId;
        private String title;
        private String color;
        private Integer position;
        private Boolean isCollapsed;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        BoardGroupDtoBuilder() {}

        public BoardGroupDtoBuilder id(String id) { this.id = id; return this; }
        public BoardGroupDtoBuilder boardId(String boardId) { this.boardId = boardId; return this; }
        public BoardGroupDtoBuilder title(String title) { this.title = title; return this; }
        public BoardGroupDtoBuilder color(String color) { this.color = color; return this; }
        public BoardGroupDtoBuilder position(Integer position) { this.position = position; return this; }
        public BoardGroupDtoBuilder isCollapsed(Boolean isCollapsed) { this.isCollapsed = isCollapsed; return this; }
        public BoardGroupDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BoardGroupDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BoardGroupDto build() {
            return new BoardGroupDto(id, boardId, title, color, position, isCollapsed, createdAt, updatedAt);
        }
    }
}
