package com.nak.backend.erp.hr.dto;

/**
 * 코드 그룹 정보 DTO (그룹 식별자 + 표시명)
 */
public class CodeGroupDto {
    private String codeGroup;
    private String codeGroupName;

    public CodeGroupDto() {}

    public CodeGroupDto(String codeGroup, String codeGroupName) {
        this.codeGroup = codeGroup;
        this.codeGroupName = codeGroupName;
    }

    public String getCodeGroup() { return codeGroup; }
    public void setCodeGroup(String codeGroup) { this.codeGroup = codeGroup; }
    public String getCodeGroupName() { return codeGroupName; }
    public void setCodeGroupName(String codeGroupName) { this.codeGroupName = codeGroupName; }
}
