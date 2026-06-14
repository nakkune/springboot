package com.nak.backend.erp.hr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 한국형 급여 ERP 시스템을 위한 다차원 DTO 구조
 * (기존 호환성 유지를 위해 PayrollDto 루트 클래스 내부에 정의하거나 기존 필드를 보존합니다)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDto {
    // ── 기존 호환성용 필드 ──
    private String id;
    private String employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private Integer payYear;
    private Integer payMonth;
    private String payDate;
    private BigDecimal basePay;
    private BigDecimal positionPay;
    private BigDecimal overtimePay;
    private BigDecimal bonusPay;
    private BigDecimal mealAllowance;
    private BigDecimal transportation;
    private BigDecimal incomeTax;
    private BigDecimal localTax;
    private BigDecimal nationalPension;
    private BigDecimal healthInsurance;
    private BigDecimal employmentInsurance;
    private BigDecimal longtermCare;
    private BigDecimal grossPay;
    private BigDecimal totalDeduction;
    private BigDecimal netPay;
    private String status;
    private String confirmedBy;
    private OffsetDateTime confirmedAt;
    private String memo;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // ── 1. 급여대장 DTO (Payroll Ledger) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollLedgerDto {
        private String id;
        private String title;
        private Integer payYear;
        private Integer payMonth;
        private String payDate;
        private String payType; // 정기급여, 상여, 성과급 등
        private String startDate;
        private String endDate;
        private String status; // draft, confirmed, paid
        private String createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        // 대시보드 합계 산출용 추가 필드
        private BigDecimal totalGross;
        private BigDecimal totalDeduction;
        private BigDecimal totalNet;
        private Integer employeeCount;
    }

    // ── 2. 수당/공제 코드 DTO (Payroll Code) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollCodeDto {
        private String code;
        private String name;
        private String type; // allowance, deduction
        private Boolean isTaxable;
        private BigDecimal taxFreeLimit;
        private Boolean isSystem;
        private Boolean isActive;
        private Integer sortOrder;
    }

    // ── 3. 사원별 기본 급여 계약 및 보험 기본설정 DTO (Salary Template) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryTemplateDto {
        private String employeeId;
        private String employeeName;
        private String employeeNo;
        private String departmentName;
        private BigDecimal basePay;
        private BigDecimal positionPay;
        private BigDecimal mealAllowance;
        private BigDecimal carAllowance;
        private Boolean useNationalPension;
        private Boolean useHealthInsurance;
        private Boolean useEmploymentInsurance;
        private Integer incomeTaxRate; // 80, 100, 120%
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    // ── 4. 급여대장 사원별 마스터 DTO (Payroll Item Summary) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollItemDto {
        private String id;
        private String ledgerId;
        private String employeeId;
        private String employeeName;   // JOIN
        private String employeeNo;     // JOIN
        private String departmentName; // JOIN
        private String position;       // JOIN
        private BigDecimal grossPay;
        private BigDecimal totalDeduction;
        private BigDecimal netPay;
        private String bankName;
        private String bankAccount;
        private String bankOwner;
        private String status;
        private String memo;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        // 해당 사원의 수당/공제 상세 리스트 (1:N 조인 맵핑)
        private List<PayrollDetailDto> details;
    }

    // ── 5. 사원별 급여 대장 지급/공제 상세 DTO (Payroll Details) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollDetailDto {
        private String id;
        private String payrollItemId;
        private String code;
        private String name;           // JOIN
        private String type;           // JOIN (allowance / deduction)
        private BigDecimal amount;
        private Boolean isTaxable;
    }
}
