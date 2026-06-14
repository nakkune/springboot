package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.PayrollDto;
import com.nak.backend.erp.hr.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/erp/hr/payrolls")
@RequiredArgsConstructor
@Tag(name = "급여 관리", description = "급여대장, 사원 급여 계산, 기본 템플릿 및 레거시 급여 정보 관리 API")
public class PayrollController {

    private final PayrollService payrollService;

    // =============================================================
    // ── [신규] 1. 급여대장 (Payroll Ledger) API                   ──
    // =============================================================
    
    @GetMapping("/ledgers")
    @Operation(summary = "급여대장 목록 조회", description = "지정된 연도, 월, 상태를 기반으로 급여대장 목록을 조회합니다.")
    public ResponseEntity<List<PayrollDto.PayrollLedgerDto>> getLedgers(
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(payrollService.getLedgers(payYear, payMonth, status));
    }

    @GetMapping("/ledgers/{id}")
    @Operation(summary = "급여대장 상세 조회", description = "ID에 해당하는 특정 급여대장의 정보를 조회합니다.")
    public ResponseEntity<PayrollDto.PayrollLedgerDto> getLedger(@PathVariable String id) {
        return ResponseEntity.ok(payrollService.getLedger(id));
    }

    @PostMapping("/ledgers")
    @Operation(summary = "급여대장 생성", description = "새로운 급여대장을 생성합니다.")
    public ResponseEntity<PayrollDto.PayrollLedgerDto> createLedger(
            @RequestBody PayrollDto.PayrollLedgerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.createLedger(dto));
    }

    @DeleteMapping("/ledgers/{id}")
    @Operation(summary = "급여대장 삭제", description = "ID에 해당하는 특정 급여대장을 삭제합니다.")
    public ResponseEntity<Void> deleteLedger(@PathVariable String id) {
        payrollService.deleteLedger(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ledgers/{id}/confirm")
    @Operation(summary = "급여대장 확정", description = "ID에 해당하는 특정 급여대장을 확정 상태로 변경합니다.")
    public ResponseEntity<Void> confirmLedger(@PathVariable String id) {
        payrollService.confirmLedger(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ledgers/{id}/pay")
    @Operation(summary = "급여대장 지급 완료 처리", description = "ID에 해당하는 특정 급여대장을 지급 완료 상태로 변경합니다.")
    public ResponseEntity<Void> payLedger(@PathVariable String id) {
        payrollService.payLedger(id);
        return ResponseEntity.ok().build();
    }


    // =============================================================
    // ── [신규] 2. 급여대장 사원 상세 및 계산 (Payroll Item) API     ──
    // =============================================================
    
    @GetMapping("/ledgers/{ledgerId}/items")
    @Operation(summary = "급여대장 내 사원 급여 항목 목록 조회", description = "특정 급여대장에 포함된 사원들의 급여 항목 목록을 조회합니다.")
    public ResponseEntity<List<PayrollDto.PayrollItemDto>> getLedgerItems(@PathVariable String ledgerId) {
        return ResponseEntity.ok(payrollService.getLedgerItems(ledgerId));
    }

    @GetMapping("/ledgers/items/{id}")
    @Operation(summary = "사원 급여 항목 상세 조회", description = "ID에 해당하는 특정 사원 급여 항목의 상세 정보를 조회합니다.")
    public ResponseEntity<PayrollDto.PayrollItemDto> getLedgerItem(@PathVariable String id) {
        return ResponseEntity.ok(payrollService.getLedgerItem(id));
    }

    @PutMapping("/ledgers/items/{id}")
    @Operation(summary = "사원 급여 항목 수정", description = "ID에 해당하는 특정 사원 급여 항목의 상세 내역(지급/공제액 등)을 수정합니다.")
    public ResponseEntity<PayrollDto.PayrollItemDto> updateLedgerItemDetails(
            @PathVariable String id,
            @RequestBody PayrollDto.PayrollItemDto dto) {
        return ResponseEntity.ok(payrollService.updateLedgerItemDetails(id, dto));
    }

    @PostMapping("/ledgers/{id}/calculate")
    @Operation(summary = "급여대장 급여 계산 실행", description = "ID에 해당하는 급여대장의 모든 사원 급여 및 세액을 계산합니다.")
    public ResponseEntity<Void> calculateLedger(@PathVariable String id) {
        payrollService.calculateLedger(id);
        return ResponseEntity.ok().build();
    }


    // =============================================================
    // ── [신규] 3. 기준 급여 코드 및 기본 템플릿 API                 ──
    // =============================================================
    
    @GetMapping("/codes")
    @Operation(summary = "급여 코드 목록 조회", description = "등록된 모든 급여/공제 항목 코드를 조회합니다.")
    public ResponseEntity<List<PayrollDto.PayrollCodeDto>> getCodes() {
        return ResponseEntity.ok(payrollService.getCodes());
    }

    @PostMapping("/codes")
    @Operation(summary = "급여 코드 저장/수정", description = "급여/공제 항목 코드를 생성하거나 수정합니다.")
    public ResponseEntity<PayrollDto.PayrollCodeDto> saveCode(@RequestBody PayrollDto.PayrollCodeDto dto) {
        return ResponseEntity.ok(payrollService.saveCode(dto));
    }

    @GetMapping("/templates")
    @Operation(summary = "기본 급여 템플릿 목록 조회", description = "등록된 모든 사원의 기본 급여 템플릿 정보를 조회합니다.")
    public ResponseEntity<List<PayrollDto.SalaryTemplateDto>> getTemplates() {
        return ResponseEntity.ok(payrollService.getTemplates());
    }

    @GetMapping("/templates/{employeeId}")
    @Operation(summary = "사원 기본 급여 템플릿 상세 조회", description = "특정 사원의 기본 급여 템플릿 정보를 조회합니다.")
    public ResponseEntity<PayrollDto.SalaryTemplateDto> getTemplate(@PathVariable String employeeId) {
        return ResponseEntity.ok(payrollService.getTemplate(employeeId));
    }

    @PostMapping("/templates")
    @Operation(summary = "기본 급여 템플릿 저장/수정", description = "사원의 기본 급여 템플릿(기본급, 수당 설정 등)을 저장하거나 수정합니다.")
    public ResponseEntity<PayrollDto.SalaryTemplateDto> saveTemplate(@RequestBody PayrollDto.SalaryTemplateDto dto) {
        return ResponseEntity.ok(payrollService.saveTemplate(dto));
    }


    // =============================================================
    // ── [호환성 보존] 레거시 erp_payrolls 기반 API                 ──
    // =============================================================
    
    @GetMapping
    @Operation(summary = "급여 정보 목록 조회 (레거시)", description = "연도, 월, 상태 및 페이지네이션을 기반으로 레거시 급여 정보 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getPayrolls(
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(payrollService.getPayrolls(payYear, payMonth, status, page, size));
    }

    @GetMapping("/employee")
    @Operation(summary = "사원별 급여 정보 목록 조회 (레거시)", description = "특정 사원의 모든 레거시 급여 내역을 조회합니다.")
    public ResponseEntity<List<PayrollDto>> getEmployeePayrolls(
            @RequestParam String employeeId) {
        return ResponseEntity.ok(payrollService.getEmployeePayrolls(employeeId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "급여 정보 단건 조회 (레거시)", description = "ID에 해당하는 특정 레거시 급여 상세 정보를 조회합니다.")
    public ResponseEntity<PayrollDto> getPayroll(@PathVariable String id) {
        return ResponseEntity.ok(payrollService.getPayroll(id));
    }

    @PostMapping
    @Operation(summary = "급여 정보 생성 (레거시)", description = "새로운 레거시 급여 정보를 생성합니다.")
    public ResponseEntity<PayrollDto> createPayroll(@RequestBody PayrollDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayroll(dto));
    }

    @PostMapping("/bulk-create")
    @Operation(summary = "급여 정보 일괄 생성 (레거시)", description = "지정된 년/월의 전체 사원 급여 정보를 일괄 생성합니다.")
    public ResponseEntity<List<PayrollDto>> bulkCreatePayroll(@RequestBody Map<String, Object> body) {
        int payYear = (int) body.get("payYear");
        int payMonth = (int) body.get("payMonth");
        String payDate = (String) body.get("payDate");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.bulkCreatePayroll(payYear, payMonth, payDate));
    }

    @PutMapping("/{id}")
    @Operation(summary = "급여 정보 수정 (레거시)", description = "ID에 해당하는 특정 레거시 급여 정보를 수정합니다.")
    public ResponseEntity<PayrollDto> updatePayroll(@PathVariable String id, @RequestBody PayrollDto dto) {
        return ResponseEntity.ok(payrollService.updatePayroll(id, dto));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "급여 정보 확정 (레거시)", description = "ID에 해당하는 특정 레거시 급여 정보를 확정 처리합니다.")
    public ResponseEntity<Void> confirmPayroll(@PathVariable String id, @RequestBody Map<String, String> body) {
        payrollService.confirmPayroll(id, body.get("confirmedBy"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "급여 지급 완료 처리 (레거시)", description = "ID에 해당하는 특정 레거시 급여 정보를 지급 완료 처리합니다.")
    public ResponseEntity<Void> markPaid(@PathVariable String id, @RequestBody Map<String, String> body) {
        payrollService.markPaid(id, body.get("confirmedBy"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "급여 정보 삭제 (레거시)", description = "ID에 해당하는 특정 레거시 급여 정보를 삭제합니다.")
    public ResponseEntity<Void> deletePayroll(@PathVariable String id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================================
    // Exception Handlers
    // =============================================================
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
