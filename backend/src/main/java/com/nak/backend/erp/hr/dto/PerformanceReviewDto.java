package com.nak.backend.erp.hr.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * ERP 인사 평가 DTO
 * erp_performance_reviews 테이블과 매핑되며,
 * employeeName, employeeNo, reviewerName은 JOIN으로 조회됩니다.
 */
public class PerformanceReviewDto {

    private String id;
    private String employeeId;
    private String employeeName;     // JOIN
    private String employeeNo;       // JOIN
    private String reviewerId;
    private String reviewerName;     // JOIN
    private Integer reviewYear;
    private String reviewPeriod;     // annual | half | quarter
    private Map<String, Object> ratings;  // JSONB
    private BigDecimal totalScore;
    private String comment;
    private String status;           // draft | submitted | acknowledged
    private OffsetDateTime submittedAt;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PerformanceReviewDto() {
        this.status = "draft";
        this.reviewPeriod = "annual";
    }

    // --- getters / setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public Integer getReviewYear() { return reviewYear; }
    public void setReviewYear(Integer reviewYear) { this.reviewYear = reviewYear; }

    public String getReviewPeriod() { return reviewPeriod; }
    public void setReviewPeriod(String reviewPeriod) { this.reviewPeriod = reviewPeriod; }

    public Map<String, Object> getRatings() { return ratings; }
    public void setRatings(Map<String, Object> ratings) { this.ratings = ratings; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
