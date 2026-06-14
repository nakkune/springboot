package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.DepartmentDto;
import com.nak.backend.erp.hr.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ERP 부서 Controller
 * 조직도 관리를 위한 REST API를 제공합니다.
 */
@Tag(name = "부서 관리", description = "ERP 조직도 및 부서 관리를 위한 REST API를 제공합니다.")
@RestController
@RequestMapping("/erp/hr/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 부서 트리 조회
     */
    @Operation(summary = "부서 트리 조회", description = "조직도 형태로 부서 트리 구조를 조회합니다.")
    @GetMapping("/tree")
    public ResponseEntity<List<DepartmentDto>> getDepartmentTree() {
        return ResponseEntity.ok(departmentService.getDepartmentTree());
    }

    /**
     * 부서 목록 조회 (평면)
     */
    @Operation(summary = "부서 목록 조회 (평면)", description = "평면 형태로 모든 부서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    /**
     * 부서 단건 조회
     */
    @Operation(summary = "부서 단건 조회", description = "부서 ID를 기준으로 부서 정보를 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable String id) {
        return ResponseEntity.ok(departmentService.getDepartment(id));
    }

    /**
     * 부서 생성
     */
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto dto) {
        DepartmentDto created = departmentService.createDepartment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 부서 수정
     */
    @Operation(summary = "부서 수정", description = "부서 ID를 기준으로 부서 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable String id, @RequestBody DepartmentDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    /**
     * 부서 삭제
     */
    @Operation(summary = "부서 삭제", description = "부서 ID를 기준으로 부서를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예외 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
