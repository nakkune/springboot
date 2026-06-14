package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.PerformanceReviewDto;
import com.nak.backend.erp.hr.mapper.PerformanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ERP 인사 평가 Service
 * 평가 CRUD와 상태 전이(draft → submitted → acknowledged)를 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceMapper performanceMapper;

    /**
     * 평가 목록 조회 (페이지네이션 + 필터)
     */
    public Map<String, Object> getReviews(String employeeId,
                                            Integer reviewYear, String reviewPeriod,
                                            String status, int page, int size) {
        int offset = (page - 1) * size;
        List<PerformanceReviewDto> items = performanceMapper.findAll(
                employeeId, reviewYear, reviewPeriod, status, offset, size);
        int total = performanceMapper.countAll(
                employeeId, reviewYear, reviewPeriod, status);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 특정 직원의 평가 목록 조회
     */
    public List<PerformanceReviewDto> getEmployeeReviews(String employeeId) {
        return performanceMapper.findByEmployeeId(employeeId);
    }

    /**
     * 평가 단건 조회
     */
    public PerformanceReviewDto getReview(String id) {
        PerformanceReviewDto dto = performanceMapper.findById(id);
        if (dto == null) {
            throw new NoSuchElementException("Performance review not found: " + id);
        }
        return dto;
    }

    /**
     * 평가 생성
     */
    @Transactional
    public PerformanceReviewDto createReview(PerformanceReviewDto dto) {
        dto.setId(UUID.randomUUID().toString());
        dto.setStatus("draft");
        dto.setCreatedAt(OffsetDateTime.now());
        dto.setUpdatedAt(OffsetDateTime.now());
        performanceMapper.insert(dto);
        return dto;
    }

    /**
     * 평가 수정 (draft 상태에서만 가능)
     */
    @Transactional
    public PerformanceReviewDto updateReview(String id, PerformanceReviewDto dto) {
        PerformanceReviewDto existing = performanceMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Performance review not found: " + id);
        }
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalStateException("Only draft reviews can be edited");
        }
        dto.setId(id);
        dto.setCreatedAt(existing.getCreatedAt());
        dto.setUpdatedAt(OffsetDateTime.now());
        dto.setStatus("draft");
        performanceMapper.update(dto);
        return dto;
    }

    /**
     * 평가 제출 (draft → submitted)
     */
    @Transactional
    public void submitReview(String id) {
        PerformanceReviewDto existing = performanceMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Performance review not found: " + id);
        }
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalStateException("Only draft reviews can be submitted");
        }
        performanceMapper.updateStatus(id, "submitted", OffsetDateTime.now());
    }

    /**
     * 평가 확인 (submitted → acknowledged)
     */
    @Transactional
    public void acknowledgeReview(String id) {
        PerformanceReviewDto existing = performanceMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Performance review not found: " + id);
        }
        if (!"submitted".equals(existing.getStatus())) {
            throw new IllegalStateException("Only submitted reviews can be acknowledged");
        }
        performanceMapper.updateStatus(id, "acknowledged", existing.getSubmittedAt());
    }

    /**
     * 평가 삭제 (draft 상태에서만 가능)
     */
    @Transactional
    public void deleteReview(String id) {
        PerformanceReviewDto existing = performanceMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Performance review not found: " + id);
        }
        if (!"draft".equals(existing.getStatus())) {
            throw new IllegalStateException("Only draft reviews can be deleted");
        }
        performanceMapper.deleteById(id);
    }
}
