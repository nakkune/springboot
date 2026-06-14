package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.DepartmentDto;
import com.nak.backend.erp.hr.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ERP 부서 Service
 * 부서 데이터를 트리 구조로 제공하며, 하위 부서가 있는 부서는 삭제되지 않도록 보호합니다.
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    /**
     * 모든 부서를 트리 구조로 조회합니다.
     */
    public List<DepartmentDto> getDepartmentTree() {
        List<DepartmentDto> allDepts = departmentMapper.findAll();
        return buildTree(allDepts);
    }

    /**
     * 모든 부서를 평면 리스트로 조회합니다.
     */
    public List<DepartmentDto> getAllDepartments() {
        return departmentMapper.findAll();
    }

    /**
     * 부서 단건 조회
     */
    public DepartmentDto getDepartment(String id) {
        DepartmentDto dept = departmentMapper.findById(id);
        if (dept == null) {
            throw new NoSuchElementException("Department not found: " + id);
        }
        return dept;
    }

    /**
     * 부서 생성
     */
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        String id = UUID.randomUUID().toString();
        dto.setId(id);
        if (dto.getSortOrder() == null) dto.setSortOrder(0);
        if (dto.getIsActive() == null) dto.setIsActive(true);
        dto.setCreatedAt(OffsetDateTime.now());
        dto.setUpdatedAt(OffsetDateTime.now());

        departmentMapper.insert(dto);
        return dto;
    }

    /**
     * 부서 수정
     */
    @Transactional
    public DepartmentDto updateDepartment(String id, DepartmentDto dto) {
        DepartmentDto existing = departmentMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Department not found: " + id);
        }

        dto.setId(id);
        dto.setCreatedAt(existing.getCreatedAt());
        dto.setUpdatedAt(OffsetDateTime.now());
        departmentMapper.update(dto);
        return dto;
    }

    /**
     * 부서 삭제 (하위 부서가 있으면 삭제 불가)
     */
    @Transactional
    public void deleteDepartment(String id) {
        DepartmentDto dept = departmentMapper.findById(id);
        if (dept == null) {
            throw new NoSuchElementException("Department not found: " + id);
        }

        List<DepartmentDto> children = departmentMapper.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete department with child departments. Delete children first.");
        }

        departmentMapper.deleteById(id);
    }

    /**
     * 평면 리스트를 트리 구조로 변환
     */
    private List<DepartmentDto> buildTree(List<DepartmentDto> flatList) {
        if (flatList == null || flatList.isEmpty()) return Collections.emptyList();

        Map<String, List<DepartmentDto>> parentChildMap = flatList.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getParentId() != null ? d.getParentId() : "",
                        Collectors.toList()
                ));

        // 루트 노드 (parentId가 null인 노드)
        List<DepartmentDto> roots = flatList.stream()
                .filter(d -> d.getParentId() == null)
                .collect(Collectors.toList());

        // 각 노드에 children 할당
        for (DepartmentDto dept : flatList) {
            String deptId = dept.getId();
            List<DepartmentDto> children = parentChildMap.getOrDefault(deptId, Collections.emptyList());
            if (!children.isEmpty()) {
                children.sort(Comparator.comparingInt(DepartmentDto::getSortOrder));
                dept.setChildren(children);
            }
        }

        roots.sort(Comparator.comparingInt(DepartmentDto::getSortOrder));
        return roots;
    }
}
