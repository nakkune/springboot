package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.AttendanceDto;
import com.nak.backend.erp.hr.service.AttendanceService;
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
@RequestMapping("/erp/hr/attendance")
@RequiredArgsConstructor
@Tag(name = "근태 관리", description = "출퇴근 기록 및 근태 현황 관리 API")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @Operation(summary = "근태 목록 조회", description = "사원 ID, 기간, 상태를 필터로 하여 근태 기록 목록을 조회합니다.")
    public ResponseEntity<List<AttendanceDto>> getAttendance(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(attendanceService.getAttendance(employeeId, fromDate, toDate, status));
    }

    @PostMapping
    @Operation(summary = "근태 기록 생성", description = "사원의 새로운 근태 기록을 생성합니다.")
    public ResponseEntity<AttendanceDto> createAttendance(@RequestBody AttendanceDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.createAttendance(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "근태 기록 단건 조회", description = "ID에 해당하는 특정 근태 기록을 조회합니다.")
    public ResponseEntity<AttendanceDto> getAttendanceById(@PathVariable String id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @PostMapping("/check-in")
    @Operation(summary = "출근 처리", description = "사원의 출근 시간 및 정보를 기록합니다.")
    public ResponseEntity<AttendanceDto> checkIn(@RequestBody Map<String, String> body) {
        AttendanceDto dto = attendanceService.checkIn(body.get("employeeId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/check-out")
    @Operation(summary = "퇴근 처리", description = "사원의 퇴근 시간 및 정보를 기록합니다.")
    public ResponseEntity<AttendanceDto> checkOut(@RequestBody Map<String, String> body) {
        AttendanceDto dto = attendanceService.checkOut(body.get("employeeId"));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "근태 기록 수정", description = "ID에 해당하는 특정 근태 기록을 수정합니다.")
    public ResponseEntity<AttendanceDto> updateAttendance(@PathVariable String id, @RequestBody AttendanceDto dto) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "근태 기록 삭제", description = "ID에 해당하는 특정 근태 기록을 삭제합니다.")
    public ResponseEntity<Void> deleteAttendance(@PathVariable String id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
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
