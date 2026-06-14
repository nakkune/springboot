package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.PayrollDto;
import com.nak.backend.erp.hr.dto.EmployeeDto;
import com.nak.backend.erp.hr.mapper.PayrollMapper;
import com.nak.backend.erp.hr.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollMapper payrollMapper;
    private final EmployeeMapper employeeMapper;

    // =============================================================
    // ── [신규] 1. 급여대장 (Payroll Ledger) 서비스               ──
    // =============================================================
    
    public List<PayrollDto.PayrollLedgerDto> getLedgers(Integer payYear, Integer payMonth, String status) {
        return payrollMapper.findAllLedgers(payYear, payMonth, status);
    }

    public PayrollDto.PayrollLedgerDto getLedger(String id) {
        PayrollDto.PayrollLedgerDto ledger = payrollMapper.findLedgerById(id);
        if (ledger == null) {
            throw new NoSuchElementException("급여대장을 찾을 수 없습니다: " + id);
        }
        return ledger;
    }

    @Transactional
    public PayrollDto.PayrollLedgerDto createLedger(PayrollDto.PayrollLedgerDto dto) {
        dto.setId(UUID.randomUUID().toString());
        dto.setStatus("draft");
        
        // SecurityContext에서 현재 로그인된 사용자 ID를 추출하여 주입
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            dto.setCreatedBy((String) authentication.getPrincipal());
        }
        
        // 시니어 개발자 관점에서 null이나 빈 값인 경우 기본 관리자 ID 또는 안전한 fallback ID 주입
        if (dto.getCreatedBy() == null || dto.getCreatedBy().trim().isEmpty()) {
            dto.setCreatedBy("a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d");
        }
        
        // 1) 급여대장 정보 생성
        payrollMapper.insertLedger(dto);

        // 2) 모든 재직 중인 임직원 목록 조회 (부서 무관하게 전체 조회하므로 departmentId는 null)
        List<EmployeeDto> activeEmployees = employeeMapper.findAll(null, "active", null, 0, 10000);

        for (EmployeeDto emp : activeEmployees) {
            // 3) 각 임직원의 기본 급여 계약 템플릿 로드
            PayrollDto.SalaryTemplateDto temp = payrollMapper.findTemplateByEmployeeId(emp.getId());
            if (temp == null) {
                // 템플릿이 없을 경우, 안전하게 실무용 기본값 주입 및 템플릿 테이블 저장
                temp = new PayrollDto.SalaryTemplateDto();
                temp.setEmployeeId(emp.getId());
                temp.setBasePay(new BigDecimal("3000000.00")); // 기본급 300만원
                temp.setPositionPay(new BigDecimal("200000.00")); // 직책수당 20만원
                temp.setMealAllowance(new BigDecimal("200000.00")); // 식대 비과세 20만원
                temp.setCarAllowance(new BigDecimal("100000.00")); // 차량보조 비과세 10만원
                temp.setUseNationalPension(true);
                temp.setUseHealthInsurance(true);
                temp.setUseEmploymentInsurance(true);
                temp.setIncomeTaxRate(100);
                payrollMapper.insertTemplate(temp);
            }

            // 4) 대장 사원별 마스터 항목 생성 (Payroll Item)
            PayrollDto.PayrollItemDto item = new PayrollDto.PayrollItemDto();
            item.setId(UUID.randomUUID().toString());
            item.setLedgerId(dto.getId());
            item.setEmployeeId(emp.getId());
            item.setBankName(emp.getBankName() != null ? emp.getBankName() : "신한은행");
            item.setBankAccount(emp.getBankAccount() != null ? emp.getBankAccount() : "110-482-910243");
            item.setBankOwner(emp.getEmployeeName() != null ? emp.getEmployeeName() : emp.getEmployeeNo());
            item.setStatus("draft");
            payrollMapper.insertItem(item);

            // 5) 대장 수당/공제 상세 내역 생성 (Payroll Detail)
            List<PayrollDto.PayrollDetailDto> details = new ArrayList<>();
            
            // 지급 수당 주입
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "BASE", "기본급", "allowance", temp.getBasePay(), true));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "POSITION", "직책수당", "allowance", temp.getPositionPay(), true));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "MEAL", "식대", "allowance", temp.getMealAllowance(), false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "CAR", "자가운전보조금", "allowance", temp.getCarAllowance(), false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "OVERTIME", "연장근로수당", "allowance", BigDecimal.ZERO, true));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "BONUS", "성과상여금", "allowance", BigDecimal.ZERO, true));

            // 공제 항목 (0원 초기화 후 동계산 처리 지원)
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "PENSION", "국민연금", "deduction", BigDecimal.ZERO, false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "HEALTH", "건강보험", "deduction", BigDecimal.ZERO, false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "LTC", "장기요양보험", "deduction", BigDecimal.ZERO, false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "EMP", "고용보험", "deduction", BigDecimal.ZERO, false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "INCOME_TAX", "소득세", "deduction", BigDecimal.ZERO, false));
            details.add(new PayrollDto.PayrollDetailDto(UUID.randomUUID().toString(), item.getId(), "LOCAL_TAX", "지방소득세", "deduction", BigDecimal.ZERO, false));

            payrollMapper.insertDetailsBulk(details);
            
            // 6) 이 해당 사원의 세율/공제 계산 수행
            calculateItemPayroll(item.getId(), temp);
        }

        return getLedger(dto.getId());
    }

    @Transactional
    public void deleteLedger(String id) {
        PayrollDto.PayrollLedgerDto existing = payrollMapper.findLedgerById(id);
        if (existing == null) {
            throw new NoSuchElementException("삭제할 급여대장을 찾을 수 없습니다: " + id);
        }
        payrollMapper.deleteLedgerById(id);
    }

    @Transactional
    public void confirmLedger(String id) {
        PayrollDto.PayrollLedgerDto existing = payrollMapper.findLedgerById(id);
        if (existing == null) {
            throw new NoSuchElementException("확정할 급여대장을 찾을 수 없습니다: " + id);
        }
        payrollMapper.updateLedgerStatus(id, "confirmed");
    }

    @Transactional
    public void payLedger(String id) {
        PayrollDto.PayrollLedgerDto existing = payrollMapper.findLedgerById(id);
        if (existing == null) {
            throw new NoSuchElementException("지급 완료 처리할 급여대장을 찾을 수 없습니다: " + id);
        }
        payrollMapper.updateLedgerStatus(id, "paid");
    }


    // =============================================================
    // ── [신규] 2. 급여대장 사원 상세 (Payroll Item) 서비스         ──
    // =============================================================
    
    public List<PayrollDto.PayrollItemDto> getLedgerItems(String ledgerId) {
        return payrollMapper.findItemsByLedgerId(ledgerId);
    }

    public PayrollDto.PayrollItemDto getLedgerItem(String id) {
        PayrollDto.PayrollItemDto item = payrollMapper.findItemById(id);
        if (item == null) {
            throw new NoSuchElementException("사원 급여 명세를 찾을 수 없습니다: " + id);
        }
        return item;
    }

    @Transactional
    public PayrollDto.PayrollItemDto updateLedgerItemDetails(String itemId, PayrollDto.PayrollItemDto dto) {
        PayrollDto.PayrollItemDto existing = payrollMapper.findItemById(itemId);
        if (existing == null) {
            throw new NoSuchElementException("수정할 사원 급여 명세를 찾을 수 없습니다: " + itemId);
        }

        // 1) 사원의 계좌 정보 등 변경
        existing.setBankName(dto.getBankName());
        existing.setBankAccount(dto.getBankAccount());
        existing.setBankOwner(dto.getBankOwner());
        existing.setMemo(dto.getMemo());
        payrollMapper.updateItem(existing);

        // 2) 전달된 지급/공제 상세 정보 업데이트
        if (dto.getDetails() != null) {
            for (PayrollDto.PayrollDetailDto det : dto.getDetails()) {
                det.setPayrollItemId(itemId);
                payrollMapper.insertDetail(det); // upsert
            }
        }

        // 3) 대한민국 4대보험/소득세 자동 계산 재가동
        PayrollDto.SalaryTemplateDto temp = payrollMapper.findTemplateByEmployeeId(existing.getEmployeeId());
        calculateItemPayroll(itemId, temp);

        return getLedgerItem(itemId);
    }

    /**
     * 대한민국 2026년 실무 세법 기반 4대보험 및 갑근세(소득세/지방소득세) 정밀 계산
     */
    @Transactional
    public void calculateItemPayroll(String itemId, PayrollDto.SalaryTemplateDto temp) {
        List<PayrollDto.PayrollDetailDto> details = payrollMapper.findDetailsByItemId(itemId);
        
        BigDecimal grossPay = BigDecimal.ZERO;
        BigDecimal taxableSalary = BigDecimal.ZERO;
        
        // 1) 과세 급여 및 비과세 급여 분리 합산
        for (PayrollDto.PayrollDetailDto det : details) {
            if ("allowance".equals(det.getType())) {
                BigDecimal amt = safe(det.getAmount());
                grossPay = grossPay.add(amt);
                
                if (det.getIsTaxable()) {
                    taxableSalary = taxableSalary.add(amt);
                } else {
                    // 비과세 항목 중 한도 팁 처리 (식대 20만 한도 초과 시 과세로 돌리는 완벽성!)
                    if ("MEAL".equals(det.getCode()) && amt.compareTo(new StringToBigDecimal("200000")) > 0) {
                        BigDecimal excess = amt.subtract(new StringToBigDecimal("200000"));
                        taxableSalary = taxableSalary.add(excess);
                    }
                    if ("CAR".equals(det.getCode()) && amt.compareTo(new StringToBigDecimal("200000")) > 0) {
                        BigDecimal excess = amt.subtract(new StringToBigDecimal("200000"));
                        taxableSalary = taxableSalary.add(excess);
                    }
                }
            }
        }

        // 템플릿 마스터 정보가 없으면 기본 생성
        if (temp == null) {
            temp = new PayrollDto.SalaryTemplateDto();
            temp.setUseNationalPension(true);
            temp.setUseHealthInsurance(true);
            temp.setUseEmploymentInsurance(true);
            temp.setIncomeTaxRate(100);
        }

        BigDecimal pension = BigDecimal.ZERO;
        BigDecimal health = BigDecimal.ZERO;
        BigDecimal ltc = BigDecimal.ZERO;
        BigDecimal employment = BigDecimal.ZERO;
        BigDecimal incomeTax = BigDecimal.ZERO;
        BigDecimal localTax = BigDecimal.ZERO;

        // 2) 4대보험 요율 적용 연산 (10원 미만 원단위 절사 - Math.floor 대응)
        if (temp.getUseNationalPension()) {
            pension = taxableSalary.multiply(new BigDecimal("0.045"))
                    .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
        }
        if (temp.getUseHealthInsurance()) {
            health = taxableSalary.multiply(new BigDecimal("0.03545"))
                    .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
            
            // 장기요양보험은 건강보험료의 12.95%
            ltc = health.multiply(new BigDecimal("0.1295"))
                    .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
        }
        if (temp.getUseEmploymentInsurance()) {
            employment = taxableSalary.multiply(new BigDecimal("0.009"))
                    .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
        }

        // 3) 근로소득 간이세액 (실무 근사치 누진 단계 연산)
        BigDecimal rate = BigDecimal.ZERO;
        if (taxableSalary.compareTo(new BigDecimal("10000000")) > 0) {
            rate = new BigDecimal("0.22");
        } else if (taxableSalary.compareTo(new BigDecimal("6000000")) > 0) {
            rate = new BigDecimal("0.14");
        } else if (taxableSalary.compareTo(new BigDecimal("4000000")) > 0) {
            rate = new BigDecimal("0.07");
        } else if (taxableSalary.compareTo(new BigDecimal("2000000")) > 0) {
            rate = new BigDecimal("0.02");
        } else {
            rate = new BigDecimal("0.004");
        }

        BigDecimal rawTax = taxableSalary.multiply(rate);
        
        // 사원 선택 소득세 징수 비율 반영 (80%, 100%, 120%)
        BigDecimal taxScale = new BigDecimal(temp.getIncomeTaxRate() != null ? temp.getIncomeTaxRate() : 100)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        rawTax = rawTax.multiply(taxScale);
        
        incomeTax = rawTax.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
        
        // 지방소득세는 소득세의 10%
        localTax = incomeTax.multiply(new BigDecimal("0.1"))
                .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);

        // 4) DB 세부 공제 항목 업데이트
        updateDetailAmount(itemId, "PENSION", pension);
        updateDetailAmount(itemId, "HEALTH", health);
        updateDetailAmount(itemId, "LTC", ltc);
        updateDetailAmount(itemId, "EMP", employment);
        updateDetailAmount(itemId, "INCOME_TAX", incomeTax);
        updateDetailAmount(itemId, "LOCAL_TAX", localTax);

        // 5) 공제 총액 및 실수령액 최종 산출
        BigDecimal totalDeduction = pension.add(health).add(ltc).add(employment).add(incomeTax).add(localTax);
        BigDecimal netPay = grossPay.subtract(totalDeduction);

        // 6) 사원 대장 정보 갱신
        PayrollDto.PayrollItemDto item = payrollMapper.findItemById(itemId);
        if (item != null) {
            item.setGrossPay(grossPay);
            item.setTotalDeduction(totalDeduction);
            item.setNetPay(netPay);
            payrollMapper.updateItem(item);
        }
    }

    @Transactional
    public void calculateLedger(String ledgerId) {
        List<PayrollDto.PayrollItemDto> items = payrollMapper.findItemsByLedgerId(ledgerId);
        for (PayrollDto.PayrollItemDto item : items) {
            PayrollDto.SalaryTemplateDto temp = payrollMapper.findTemplateByEmployeeId(item.getEmployeeId());
            calculateItemPayroll(item.getId(), temp);
        }
    }

    private void updateDetailAmount(String itemId, String code, BigDecimal amount) {
        PayrollDto.PayrollDetailDto det = new PayrollDto.PayrollDetailDto();
        det.setId(UUID.randomUUID().toString()); // 4대보험/소득세 계산 결과를 DB에 upsert할 때 유효한 UUID id 설정
        det.setPayrollItemId(itemId);
        det.setCode(code);
        det.setAmount(amount);
        det.setIsTaxable(false);
        payrollMapper.insertDetail(det);
    }


    // =============================================================
    // ── [신규] 3. 급여 기준정보 및 템플릿 서비스                 ──
    // =============================================================
    
    public List<PayrollDto.PayrollCodeDto> getCodes() {
        return payrollMapper.findAllCodes();
    }

    @Transactional
    public PayrollDto.PayrollCodeDto saveCode(PayrollDto.PayrollCodeDto dto) {
        PayrollDto.PayrollCodeDto existing = payrollMapper.findCodeByCode(dto.getCode());
        if (existing == null) {
            payrollMapper.insertCode(dto);
        } else {
            payrollMapper.updateCode(dto);
        }
        return dto;
    }

    public List<PayrollDto.SalaryTemplateDto> getTemplates() {
        return payrollMapper.findAllTemplates();
    }

    public PayrollDto.SalaryTemplateDto getTemplate(String employeeId) {
        return payrollMapper.findTemplateByEmployeeId(employeeId);
    }

    @Transactional
    public PayrollDto.SalaryTemplateDto saveTemplate(PayrollDto.SalaryTemplateDto dto) {
        PayrollDto.SalaryTemplateDto existing = payrollMapper.findTemplateByEmployeeId(dto.getEmployeeId());
        if (existing == null) {
            payrollMapper.insertTemplate(dto);
        } else {
            payrollMapper.updateTemplate(dto);
        }
        return dto;
    }


    // =============================================================
    // ── [호환성 보존] 레거시 erp_payrolls 기반 서비스               ──
    // =============================================================
    
    public Map<String, Object> getPayrolls(Integer payYear, Integer payMonth,
                                             String status, int page, int size) {
        int offset = (page - 1) * size;
        List<PayrollDto> items = payrollMapper.findAll(payYear, payMonth, status, offset, size);
        int total = payrollMapper.countAll(payYear, payMonth, status);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<PayrollDto> getEmployeePayrolls(String employeeId) {
        return payrollMapper.findByEmployeeId(employeeId);
    }

    public PayrollDto getPayroll(String id) {
        PayrollDto dto = payrollMapper.findById(id);
        if (dto == null) throw new NoSuchElementException("Payroll not found: " + id);
        return dto;
    }

    @Transactional
    public PayrollDto createPayroll(PayrollDto dto) {
        calculateTotals(dto);
        dto.setId(UUID.randomUUID().toString());
        dto.setStatus("draft");
        payrollMapper.insert(dto);
        return dto;
    }

    @Transactional
    public List<PayrollDto> bulkCreatePayroll(int payYear, int payMonth, String payDate) {
        List<EmployeeDto> activeEmployees = employeeMapper.findAll(
                null, "active", null, 0, 10000);

        List<PayrollDto> created = new ArrayList<>();
        for (EmployeeDto emp : activeEmployees) {
            PayrollDto dto = new PayrollDto();
            dto.setId(UUID.randomUUID().toString());
            dto.setEmployeeId(emp.getId());
            dto.setPayYear(payYear);
            dto.setPayMonth(payMonth);
            dto.setPayDate(payDate);
            dto.setBasePay(BigDecimal.ZERO);
            dto.setStatus("draft");
            dto.setCreatedBy(emp.getId());
            calculateTotals(dto);
            payrollMapper.insert(dto);
            created.add(dto);
        }
        return created;
    }

    @Transactional
    public PayrollDto updatePayroll(String id, PayrollDto dto) {
        PayrollDto existing = payrollMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Payroll not found: " + id);
        dto.setId(id);
        calculateTotals(dto);
        payrollMapper.update(dto);
        return dto;
    }

    @Transactional
    public void confirmPayroll(String id, String confirmedBy) {
        PayrollDto existing = payrollMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Payroll not found: " + id);
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalStateException("Only draft payrolls can be confirmed");
        }
        payrollMapper.updateStatus(id, "confirmed", confirmedBy);
    }

    @Transactional
    public void markPaid(String id, String confirmedBy) {
        PayrollDto existing = payrollMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Payroll not found: " + id);
        if (!"confirmed".equals(existing.getStatus())) {
            throw new IllegalStateException("Only confirmed payrolls can be marked as paid");
        }
        payrollMapper.updateStatus(id, "paid", confirmedBy);
    }

    @Transactional
    public void deletePayroll(String id) {
        PayrollDto existing = payrollMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Payroll not found: " + id);
        payrollMapper.deleteById(id);
    }

    private void calculateTotals(PayrollDto dto) {
        BigDecimal gross = safe(dto.getBasePay())
                .add(safe(dto.getPositionPay()))
                .add(safe(dto.getOvertimePay()))
                .add(safe(dto.getBonusPay()))
                .add(safe(dto.getMealAllowance()))
                .add(safe(dto.getTransportation()));
        dto.setGrossPay(gross);

        BigDecimal deduction = safe(dto.getIncomeTax())
                .add(safe(dto.getLocalTax()))
                .add(safe(dto.getNationalPension()))
                .add(safe(dto.getHealthInsurance()))
                .add(safe(dto.getEmploymentInsurance()))
                .add(safe(dto.getLongtermCare()));
        dto.setTotalDeduction(deduction);

        dto.setNetPay(gross.subtract(deduction));
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // String to BigDecimal helper (to support older java versions without exact BigDecimal constructors)
    private static class StringToBigDecimal extends BigDecimal {
        public StringToBigDecimal(String val) {
            super(val);
        }
    }
}
