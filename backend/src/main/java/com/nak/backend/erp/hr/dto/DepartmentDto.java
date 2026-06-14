package com.nak.backend.erp.hr.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * ERP 부서 DTO
 * 조직도 트리 구조를 표현하기 위해 children 필드를 포함합니다.
 */
public class DepartmentDto {
    private String id;
    private String parentId;
    private String name;
    private String code;
    private String managerId;
    private Integer sortOrder;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 트리 구조용 (DB 매핑 제외, transient)
    private List<DepartmentDto> children;

    public DepartmentDto() {
        this.isActive = true;
        this.sortOrder = 0;
    }

    public DepartmentDto(String id, String parentId, String name, String code,
                         String managerId, Integer sortOrder, Boolean isActive,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.code = code;
        this.managerId = managerId;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<DepartmentDto> getChildren() { return children; }
    public void setChildren(List<DepartmentDto> children) { this.children = children; }
}
