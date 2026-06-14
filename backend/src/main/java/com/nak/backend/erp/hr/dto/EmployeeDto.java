package com.nak.backend.erp.hr.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * ERP 직원 마스터 DTO
 * erp_employees 테이블과 매핑되며, departmentName은 JOIN으로 조회됩니다.
 */
public class EmployeeDto {
    private String id;
    private String userId;
    private String employeeNo;
    private String departmentId;
    private String departmentName; // JOIN 결과 (DB에 없는 transient)
    private String employeeName;   // JOIN 결과 (DB에 없는 transient)
    private String position;
    private String jobTitle;
    private String employmentType;
    private String hireDate;
    private String resignationDate;
    private String status;
    private String phone;
    private String email;
    private String emergencyContact;
    private String emergencyPhone;
    private String bankName;
    private String bankAccount;
    private BigDecimal annualLeaveDays;
    private String memo;
    private String leaveStartDate;
    private String leaveEndDate;
    private String birthDate;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public EmployeeDto() {
        this.employmentType = "full_time";
        this.status = "active";
        this.annualLeaveDays = BigDecimal.valueOf(15);
    }

    public EmployeeDto(String id, String userId, String employeeName, String employeeNo,
                       String departmentId, String departmentName, String position, String jobTitle,
                       String employmentType, String hireDate, String resignationDate, String status,
                       String phone, String email, String emergencyContact, String emergencyPhone,
                       String bankName, String bankAccount, BigDecimal annualLeaveDays, String memo,
                       String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.employeeNo = employeeNo;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.position = position;
        this.jobTitle = jobTitle;
        this.employmentType = employmentType != null ? employmentType : "full_time";
        this.hireDate = hireDate;
        this.resignationDate = resignationDate;
        this.status = status != null ? status : "active";
        this.phone = phone;
        this.email = email;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.annualLeaveDays = annualLeaveDays != null ? annualLeaveDays : BigDecimal.valueOf(15);
        this.memo = memo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public String getResignationDate() { return resignationDate; }
    public void setResignationDate(String resignationDate) { this.resignationDate = resignationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public BigDecimal getAnnualLeaveDays() { return annualLeaveDays; }
    public void setAnnualLeaveDays(BigDecimal annualLeaveDays) { this.annualLeaveDays = annualLeaveDays; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getLeaveStartDate() { return leaveStartDate; }
    public void setLeaveStartDate(String leaveStartDate) { this.leaveStartDate = leaveStartDate; }
    public String getLeaveEndDate() { return leaveEndDate; }
    public void setLeaveEndDate(String leaveEndDate) { this.leaveEndDate = leaveEndDate; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}
