package com.nak.backend.erp.hr.dto;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

public class AttendanceDto {
    private String id;
    private String employeeId;
    private String employeeName;  // JOIN
    private String employeeNo;    // JOIN
    private String workDate;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String status;        // present | late | early_leave | absent
    private String memo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AttendanceDto() {
        this.status = "present";
        this.workHours = BigDecimal.ZERO;
        this.overtimeHours = BigDecimal.ZERO;
    }

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }
    public OffsetDateTime getCheckIn() { return checkIn; }
    public void setCheckIn(OffsetDateTime checkIn) { this.checkIn = checkIn; }
    public OffsetDateTime getCheckOut() { return checkOut; }
    public void setCheckOut(OffsetDateTime checkOut) { this.checkOut = checkOut; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
