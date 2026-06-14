package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.EmployeeDto;
import com.nak.backend.erp.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ERP 직원 Controller
 * 직원 마스터 관리를 위한 REST API를 제공합니다.
 */
@Tag(name = "직원 관리", description = "ERP 직원 마스터 관리를 위한 REST API를 제공합니다.")
@RestController
@RequestMapping("/erp/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 직원 목록 조회 (페이지네이션 + 필터)
     */
    @Operation(summary = "직원 목록 조회", description = "페이지네이션 및 필터를 적용하여 직원 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getEmployees(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(employeeService.getEmployees(departmentId, status, search, page, size));
    }

    /**
     * 직원 단건 조회
     */
    @Operation(summary = "직원 단건 조회", description = "직원 ID를 기준으로 직원 정보를 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    /**
     * user_id를 기준으로 직원 정보 단건 조회
     */
    @Operation(summary = "사용자 ID 기준 직원 조회", description = "사용자 ID(user_id)를 기준으로 직원 정보를 상세 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<EmployeeDto> getEmployeeByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(employeeService.getEmployeeByUserId(userId));
    }

    /**
     * 직원 생성
     */
    @Operation(summary = "직원 생성", description = "새로운 직원 정보를 생성합니다.")
    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody EmployeeDto dto) {
        EmployeeDto created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 직원 수정
     */
    @Operation(summary = "직원 수정", description = "직원 ID를 기준으로 직원 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable String id, @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }

    /**
     * 직원 상태 변경
     */
    @Operation(summary = "직원 상태 변경", description = "직원의 상태(재직, 퇴사 등)를 변경합니다.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateEmployeeStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        employeeService.updateEmployeeStatus(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    /**
     * 직원 삭제
     */
    @Operation(summary = "직원 삭제", description = "직원 ID를 기준으로 직원 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예외 처리
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
