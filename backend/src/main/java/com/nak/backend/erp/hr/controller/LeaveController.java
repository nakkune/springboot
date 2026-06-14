package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.LeaveBalanceDto;
import com.nak.backend.erp.hr.dto.LeaveRequestDto;
import com.nak.backend.erp.hr.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "휴가 관리", description = "ERP 휴가 신청, 승인/반려 및 잔여 일수 관리를 위한 REST API를 제공합니다.")
@RestController
@RequestMapping("/erp/hr/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "휴가 신청 목록 조회", description = "신청자 ID, 결재자 ID, 또는 상태별로 휴가 신청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<LeaveRequestDto>> getLeaves(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String managerId,
            @RequestParam(required = false) String status) {
        if (managerId != null && !managerId.isEmpty()) {
            return ResponseEntity.ok(leaveService.getLeavesByManager(managerId, status));
        }
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employeeId, status));
    }

    @Operation(summary = "휴가 상세 조회", description = "휴가 신청 ID를 기준으로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> getLeave(@PathVariable String id) {
        return ResponseEntity.ok(leaveService.getLeave(id));
    }

    @Operation(summary = "휴가 신청 생성", description = "새로운 휴가 신청을 생성합니다.")
    @PostMapping
    public ResponseEntity<LeaveRequestDto> createLeave(@RequestBody LeaveRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.createLeave(dto));
    }

    @Operation(summary = "휴가 신청 수정", description = "휴가 신청 ID를 기준으로 신청 내용을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> updateLeave(@PathVariable String id, @RequestBody LeaveRequestDto dto) {
        return ResponseEntity.ok(leaveService.updateLeave(id, dto));
    }

    @Operation(summary = "휴가 승인", description = "휴가 신청을 승인 처리합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveLeave(@PathVariable String id, @RequestBody Map<String, String> body) {
        leaveService.approveLeave(id, body.get("approverId"));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "휴가 반려", description = "휴가 신청을 반려 처리합니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectLeave(@PathVariable String id, @RequestBody Map<String, String> body) {
        leaveService.rejectLeave(id, body.get("approverId"), body.get("rejectReason"));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "휴가 신청 취소", description = "휴가 신청을 취소 처리합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelLeave(@PathVariable String id) {
        leaveService.cancelLeave(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "휴가 신청 삭제", description = "휴가 신청 기록을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeave(@PathVariable String id) {
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }

    // Leave Balance
    @Operation(summary = "휴가 잔여 일수 조회", description = "특정 직원의 특정 연도 기준 휴가 잔여 일수 정보를 조회합니다.")
    @GetMapping("/balance")
    public ResponseEntity<LeaveBalanceDto> getLeaveBalance(
            @RequestParam String employeeId,
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = java.time.Year.now().getValue();
        return ResponseEntity.ok(leaveService.getLeaveBalance(employeeId, year));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
