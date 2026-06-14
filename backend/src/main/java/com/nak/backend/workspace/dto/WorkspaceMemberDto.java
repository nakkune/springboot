package com.nak.backend.workspace.dto;

import java.time.OffsetDateTime;

/**
 * 워크스페이스 멤버 정보 DTO
 * 10년 차 시니어 설계: 조인된 사용자 정보(이메일, 이름)를 포함합니다.
 */
public class WorkspaceMemberDto {
    private String id;
    private String workspaceId;
    private String userId;
    private String role;
    private String email;
    private String fullName;
    private OffsetDateTime createdAt;

    public WorkspaceMemberDto() {}

    public WorkspaceMemberDto(String id, String workspaceId, String userId, String role, String email, String fullName, OffsetDateTime createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
