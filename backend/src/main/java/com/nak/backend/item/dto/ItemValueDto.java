package com.nak.backend.item.dto;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 아이템 컬럼 값 정보 DTO (EAV 패턴)
 */
public class ItemValueDto {
    private String id;
    private String itemId;
    private String columnId;
    private String valueText;
    private BigDecimal valueNumber;
    private LocalDate valueDate;
    private String valueJson;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public ItemValueDto() {}

    public ItemValueDto(String id, String itemId, String columnId, String valueText, BigDecimal valueNumber, LocalDate valueDate, String valueJson, String updatedBy, OffsetDateTime updatedAt) {
        this.id = id;
        this.itemId = itemId;
        this.columnId = columnId;
        this.valueText = valueText;
        this.valueNumber = valueNumber;
        this.valueDate = valueDate;
        this.valueJson = valueJson;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getColumnId() { return columnId; }
    public void setColumnId(String columnId) { this.columnId = columnId; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public BigDecimal getValueNumber() { return valueNumber; }
    public void setValueNumber(BigDecimal valueNumber) { this.valueNumber = valueNumber; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ItemValueDtoBuilder builder() { return new ItemValueDtoBuilder(); }

    public static class ItemValueDtoBuilder {
        private String id;
        private String itemId;
        private String columnId;
        private String valueText;
        private BigDecimal valueNumber;
        private LocalDate valueDate;
        private String valueJson;
        private String updatedBy;
        private OffsetDateTime updatedAt;

        ItemValueDtoBuilder() {}

        public ItemValueDtoBuilder id(String id) { this.id = id; return this; }
        public ItemValueDtoBuilder itemId(String itemId) { this.itemId = itemId; return this; }
        public ItemValueDtoBuilder columnId(String columnId) { this.columnId = columnId; return this; }
        public ItemValueDtoBuilder valueText(String valueText) { this.valueText = valueText; return this; }
        public ItemValueDtoBuilder valueNumber(BigDecimal valueNumber) { this.valueNumber = valueNumber; return this; }
        public ItemValueDtoBuilder valueDate(LocalDate valueDate) { this.valueDate = valueDate; return this; }
        public ItemValueDtoBuilder valueJson(String valueJson) { this.valueJson = valueJson; return this; }
        public ItemValueDtoBuilder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public ItemValueDtoBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ItemValueDto build() {
            return new ItemValueDto(id, itemId, columnId, valueText, valueNumber, valueDate, valueJson, updatedBy, updatedAt);
        }
    }
}
