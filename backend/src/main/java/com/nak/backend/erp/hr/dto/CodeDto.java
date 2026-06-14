package com.nak.backend.erp.hr.dto;

import java.time.OffsetDateTime;

/**
 * ERP 공통 코드 DTO
 * erp_codes 테이블과 매핑되어 직위, 고용형태, 휴가유형 등 기준정보를 관리합니다.
 */
public class CodeDto {
    private String id;
    private String codeGroup;
    private String codeGroupName;
    private String code;
    private String label;
    private Integer sortOrder;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CodeDto() {
        this.isActive = true;
        this.sortOrder = 0;
    }

    public CodeDto(String id, String codeGroup, String codeGroupName, String code, String label,
                   Integer sortOrder, Boolean isActive,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.codeGroup = codeGroup;
        this.codeGroupName = codeGroupName;
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodeGroup() { return codeGroup; }
    public void setCodeGroup(String codeGroup) { this.codeGroup = codeGroup; }
    public String getCodeGroupName() { return codeGroupName; }
    public void setCodeGroupName(String codeGroupName) { this.codeGroupName = codeGroupName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
