package com.nak.backend.item.dto;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 아이템(태스크) 정보 DTO
 */
public class ItemDto {
    private String id;
    private String boardId;
    private String groupId;
    private String parentItemId;
    private String name;
    private Integer position;
    private Boolean isArchived;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // [NEW] EAV 상태값 및 담당자 매핑을 담기 위한 동적 밸류 맵 (10년 차 시니어 설계)
    private Map<String, String> values;

    public ItemDto() {
        this.position = 0;
        this.isArchived = false;
        this.values = new HashMap<>();
    }

    public ItemDto(String id, String boardId, String groupId, String parentItemId, String name, Integer position, Boolean isArchived, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.groupId = groupId;
        this.parentItemId = parentItemId;
        this.name = name;
        this.position = position != null ? position : 0;
        this.isArchived = isArchived != null ? isArchived : false;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.values = new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getParentItemId() { return parentItemId; }
    public void setParentItemId(String parentItemId) { this.parentItemId = parentItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    
    // [NEW] values getter/setter
    public Map<String, String> getValues() { return values; }
    public void setValues(Map<String, String> values) { this.values = values != null ? values : new HashMap<>(); }

    public static ItemDtoBuilder builder() { return new ItemDtoBuilder(); }

    public static class ItemDtoBuilder {
        private String id;
        private String boardId;
        private String groupId;
        private String parentItemId;
        private String name;
        private Integer position;
        private Boolean isArchived;
        private String createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private Map<String, String> values;

        ItemDtoBuilder() {}

        public ItemDtoBuilder id(String id) { this.id = id; return this; }
        public ItemDtoBuilder boardId(String boardId) { this.boardId = boardId; return this; }
        public ItemDtoBuilder groupId(String groupId) { this.groupId = groupId; return this; }
        public ItemDtoBuilder parentItemId(String parentItemId) { this.parentItemId = parentItemId; return this; }
        public ItemDtoBuilder name(String name) { this.name = name; return this; }
        public ItemDtoBuilder position(Integer position) { this.position = position; return this; }
        public ItemDtoBuilder isArchived(Boolean isArchived) { this.isArchived = isArchived; return this; }
        public ItemDtoBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public ItemDtoBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ItemDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ItemDtoBuilder values(Map<String, String> values) { this.values = values; return this; }

        public ItemDto build() {
            ItemDto itemDto = new ItemDto(id, boardId, groupId, parentItemId, name, position, isArchived, createdBy, createdAt, updatedAt);
            if (this.values != null) {
                itemDto.setValues(this.values);
            }
            return itemDto;
        }
    }
}
