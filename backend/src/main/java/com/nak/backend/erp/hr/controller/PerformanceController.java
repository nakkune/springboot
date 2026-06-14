package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.PerformanceReviewDto;
import com.nak.backend.erp.hr.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * ERP 인사 평가 Controller
 * 평가 CRUD 및 상태 전이(제출/확인) REST API를 제공합니다.
 */
@RestController
@RequestMapping("/erp/hr/reviews")
@RequiredArgsConstructor
@Tag(name = "인사 평가 관리", description = "인사 평가 생성, 수정, 삭제, 조회 및 상태 전이(제출/확인) API")
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * 평가 목록 조회 (페이지네이션 + 필터)
     */
    @GetMapping
    @Operation(summary = "평가 목록 조회", description = "사원 ID, 평가 연도, 평가 구분, 상태 및 페이지네이션을 기반으로 평가 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getReviews(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Integer reviewYear,
            @RequestParam(required = false) String reviewPeriod,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(performanceService.getReviews(
                employeeId, reviewYear, reviewPeriod, status, page, size));
    }

    /**
     * 특정 직원의 평가 목록 조회
     */
    @GetMapping("/employee")
    @Operation(summary = "사원별 평가 목록 조회", description = "특정 사원의 모든 인사 평가 내역을 조회합니다.")
    public ResponseEntity<List<PerformanceReviewDto>> getEmployeeReviews(
            @RequestParam String employeeId) {
        return ResponseEntity.ok(performanceService.getEmployeeReviews(employeeId));
    }

    /**
     * 평가 단건 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "평가 상세 조회", description = "ID에 해당하는 특정 인사 평가의 상세 정보를 조회합니다.")
    public ResponseEntity<PerformanceReviewDto> getReview(@PathVariable String id) {
        return ResponseEntity.ok(performanceService.getReview(id));
    }

    /**
     * 평가 생성
     */
    @PostMapping
    @Operation(summary = "평가 작성", description = "새로운 인사 평가를 작성합니다.")
    public ResponseEntity<PerformanceReviewDto> createReview(@RequestBody PerformanceReviewDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createReview(dto));
    }

    /**
     * 평가 수정
     */
    @PutMapping("/{id}")
    @Operation(summary = "평가 수정", description = "ID에 해당하는 특정 인사 평가를 수정합니다.")
    public ResponseEntity<PerformanceReviewDto> updateReview(
            @PathVariable String id, @RequestBody PerformanceReviewDto dto) {
        return ResponseEntity.ok(performanceService.updateReview(id, dto));
    }

    /**
     * 평가 제출 (draft → submitted)
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "평가 제출", description = "ID에 해당하는 특정 인사 평가를 작성 중(draft)에서 제출 완료(submitted) 상태로 변경합니다.")
    public ResponseEntity<Void> submitReview(@PathVariable String id) {
        performanceService.submitReview(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 평가 확인 (submitted → acknowledged)
     */
    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "평가 확인", description = "ID에 해당하는 특정 인사 평가를 제출 완료(submitted)에서 본인 확인 완료(acknowledged) 상태로 변경합니다.")
    public ResponseEntity<Void> acknowledgeReview(@PathVariable String id) {
        performanceService.acknowledgeReview(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 평가 삭제
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "평가 삭제", description = "ID에 해당하는 특정 인사 평가를 삭제합니다.")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        performanceService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }
}
