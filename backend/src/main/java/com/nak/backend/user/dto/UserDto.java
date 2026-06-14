package com.nak.backend.user.dto;

import java.time.OffsetDateTime;

/**
 * 사용자 정보 DTO
 * 10년 차 시니어 관점에서 PostgreSQL schema.sql 정의와 유기적인 매핑을 설계하였으며,
 * TIMESTAMPTZ 타입에 맞추어 시간대 오프셋을 명확히 보존할 수 있도록 OffsetDateTime을 활용합니다.
 */
public class UserDto {

    private String id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private String timezone;
    private String role;           // 사용자 권한 ('member' | 'moderator' | 'admin')
    private String memberStatus;   // 가입 승인 상태 ('pending' | 'approved' | 'rejected')
    private String theme;          // [NEW] 시스템 테마 스킨 ('dark' | 'light')
    private Boolean isActive;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 기본 생성자
    public UserDto() {
        this.timezone = "Asia/Seoul";
        this.role = "member";
        this.memberStatus = "approved";
        this.theme = "dark";
        this.isActive = true;
    }

    // 전체 매개변수 생성자
    public UserDto(String id, String email, String passwordHash, String fullName, String avatarUrl, 
                   String timezone, String role, String memberStatus, String theme, Boolean isActive, 
                   OffsetDateTime lastLoginAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.timezone = timezone != null ? timezone : "Asia/Seoul";
        this.role = role != null ? role : "member";
        this.memberStatus = memberStatus != null ? memberStatus : "approved";
        this.theme = theme != null ? theme : "dark";
        this.isActive = isActive != null ? isActive : true;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 명시적 빌더 구현
    public static UserDtoBuilder builder() {
        return new UserDtoBuilder();
    }

    public static class UserDtoBuilder {
        private String id;
        private String email;
        private String passwordHash;
        private String fullName;
        private String avatarUrl;
        private String timezone;
        private String role;
        private String memberStatus;
        private String theme;
        private Boolean isActive;
        private OffsetDateTime lastLoginAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        UserDtoBuilder() {}

        public UserDtoBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserDtoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserDtoBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserDtoBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserDtoBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserDtoBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public UserDtoBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserDtoBuilder memberStatus(String memberStatus) {
            this.memberStatus = memberStatus;
            return this;
        }

        public UserDtoBuilder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public UserDtoBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public UserDtoBuilder lastLoginAt(OffsetDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public UserDtoBuilder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDtoBuilder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserDto build() {
            return new UserDto(id, email, passwordHash, fullName, avatarUrl, timezone, role, memberStatus, theme, isActive, lastLoginAt, createdAt, updatedAt);
        }
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", timezone='" + timezone + '\'' +
                ", role='" + role + '\'' +
                ", memberStatus='" + memberStatus + '\'' +
                ", theme='" + theme + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}




