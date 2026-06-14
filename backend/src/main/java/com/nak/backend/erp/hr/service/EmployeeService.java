package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.EmployeeDto;
import com.nak.backend.erp.hr.mapper.EmployeeMapper;
import com.nak.backend.erp.common.util.DocumentNoGenerator;
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
 * ERP 직원 Service
 * 직원 마스터 데이터를 관리하며, 페이지네이션과 상태 변경을 지원합니다.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;

    /**
     * 직원 목록 조회 (페이지네이션 + 필터)
     */
    public Map<String, Object> getEmployees(String departmentId,
                                             String status, String search, int page, int size) {
        int offset = (page - 1) * size;
        List<EmployeeDto> items = employeeMapper.findAll(departmentId, status, search, offset, size);
        int total = employeeMapper.countAll(departmentId, status, search);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 직원 단건 조회
     */
    public EmployeeDto getEmployee(String id) {
        EmployeeDto emp = employeeMapper.findById(id);
        if (emp == null) {
            throw new NoSuchElementException("Employee not found: " + id);
        }
        return emp;
    }

    /**
     * user_id를 기준으로 직원 정보 조회
     */
    public EmployeeDto getEmployeeByUserId(String userId) {
        EmployeeDto emp = employeeMapper.findByUserId(userId);
        if (emp == null) {
            throw new NoSuchElementException("Employee not found for userId: " + userId);
        }
        return emp;
    }

    /**
     * 직원 생성 (자동 사번 발급 - YYMMDD001 형식)
     */
    @Transactional
    public EmployeeDto createEmployee(EmployeeDto dto) {
        String id = UUID.randomUUID().toString();
        dto.setId(id);

        // 사번이 전달되지 않았거나 빈 경우 자동 채번 가동 (YYMMDD + 3자리 순번)
        if (dto.getEmployeeNo() == null || dto.getEmployeeNo().isBlank()) {
            java.time.LocalDate today = java.time.LocalDate.now();
            String yymmdd = today.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            String lastNo = employeeMapper.findLastEmployeeNoByPattern(yymmdd);
            
            String nextNo;
            if (lastNo != null && lastNo.length() >= 9) {
                try {
                    int seq = Integer.parseInt(lastNo.substring(6)) + 1;
                    nextNo = yymmdd + String.format("%03d", seq);
                } catch (NumberFormatException e) {
                    nextNo = yymmdd + "001";
                }
            } else {
                nextNo = yymmdd + "001";
            }
            dto.setEmployeeNo(nextNo);
        }

        dto.setCreatedAt(OffsetDateTime.now());
        dto.setUpdatedAt(OffsetDateTime.now());

        employeeMapper.insert(dto);
        return dto;
    }

    /**
     * 직원 정보 수정
     */
    @Transactional
    public EmployeeDto updateEmployee(String id, EmployeeDto dto) {
        EmployeeDto existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Employee not found: " + id);
        }

        dto.setId(id);
        dto.setCreatedAt(existing.getCreatedAt());
        dto.setUpdatedAt(OffsetDateTime.now());
        dto.setEmployeeNo(existing.getEmployeeNo()); // 사번 유지

        employeeMapper.update(dto);
        return dto;
    }

    /**
     * 직원 상태 변경 (active / leave / resigned)
     */
    @Transactional
    public void updateEmployeeStatus(String id, String status) {
        EmployeeDto existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Employee not found: " + id);
        }
        employeeMapper.updateStatus(id, status);
    }

    /**
     * 직원 삭제
     */
    @Transactional
    public void deleteEmployee(String id) {
        EmployeeDto existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Employee not found: " + id);
        }
        employeeMapper.deleteById(id);
    }
}
