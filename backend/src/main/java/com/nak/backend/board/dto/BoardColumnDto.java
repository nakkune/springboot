package com.nak.backend.board.dto;

import java.time.OffsetDateTime;

/**
 * 보드 컬럼 정보 DTO
 */
public class BoardColumnDto {
    private String id;
    private String boardId;
    private String name;
    private String columnType;
    private String settings;
    private Integer position;
    private Boolean isRequired;
    private Boolean isHidden;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public BoardColumnDto() {
        this.settings = "{}";
        this.position = 0;
        this.isRequired = false;
        this.isHidden = false;
    }

    public BoardColumnDto(String id, String boardId, String name, String columnType, String settings, Integer position, Boolean isRequired, Boolean isHidden, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.name = name;
        this.columnType = columnType;
        this.settings = settings != null ? settings : "{}";
        this.position = position != null ? position : 0;
        this.isRequired = isRequired != null ? isRequired : false;
        this.isHidden = isHidden != null ? isHidden : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColumnType() { return columnType; }
    public void setColumnType(String columnType) { this.columnType = columnType; }
    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static BoardColumnDtoBuilder builder() { return new BoardColumnDtoBuilder(); }

    public static class BoardColumnDtoBuilder {
        private String id;
        private String boardId;
        private String name;
        private String columnType;
        private String settings;
        private Integer position;
        private Boolean isRequired;
        private Boolean isHidden;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        BoardColumnDtoBuilder() {}

        public BoardColumnDtoBuilder id(String id) { this.id = id; return this; }
        public BoardColumnDtoBuilder boardId(String boardId) { this.boardId = boardId; return this; }
        public BoardColumnDtoBuilder name(String name) { this.name = name; return this; }
        public BoardColumnDtoBuilder columnType(String columnType) { this.columnType = columnType; return this; }
        public BoardColumnDtoBuilder settings(String settings) { this.settings = settings; return this; }
        public BoardColumnDtoBuilder position(Integer position) { this.position = position; return this; }
        public BoardColumnDtoBuilder isRequired(Boolean isRequired) { this.isRequired = isRequired; return this; }
        public BoardColumnDtoBuilder isHidden(Boolean isHidden) { this.isHidden = isHidden; return this; }
        public BoardColumnDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BoardColumnDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BoardColumnDto build() {
            return new BoardColumnDto(id, boardId, name, columnType, settings, position, isRequired, isHidden, createdAt, updatedAt);
        }
    }
}
