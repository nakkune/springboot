package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.CodeDto;
import com.nak.backend.erp.hr.dto.CodeGroupDto;
import com.nak.backend.erp.hr.mapper.CodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ERP 공통 코드 Service
 * 기준정보(직위, 고용형태, 휴가유형 등)를 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeMapper codeMapper;

    /**
     * 모든 코드 그룹 목록 조회 (식별자 + 표시명)
     */
    public List<CodeGroupDto> getGroups() {
        return codeMapper.findGroups();
    }

    /**
     * 특정 그룹의 코드 목록 조회
     */
    public List<CodeDto> getCodes(String codeGroup) {
        return codeMapper.findByGroup(codeGroup);
    }

    /**
     * 모든 코드 조회 (그룹별 정렬)
     */
    public List<CodeDto> getAllCodes() {
        return codeMapper.findAll();
    }

    /**
     * 코드 단건 조회
     */
    public CodeDto getCode(String id) {
        CodeDto code = codeMapper.findById(id);
        if (code == null) {
            throw new NoSuchElementException("Code not found: " + id);
        }
        return code;
    }

    /**
     * 코드 생성
     */
    @Transactional
    public CodeDto createCode(CodeDto dto) {
        String id = UUID.randomUUID().toString();
        dto.setId(id);
        if (dto.getSortOrder() == null) dto.setSortOrder(0);
        if (dto.getIsActive() == null) dto.setIsActive(true);
        if (dto.getCodeGroupName() == null) dto.setCodeGroupName("");
        dto.setCreatedAt(OffsetDateTime.now());
        dto.setUpdatedAt(OffsetDateTime.now());

        // 중복 체크
        CodeDto existing = codeMapper.findByGroupAndCode(dto.getCodeGroup(), dto.getCode());
        if (existing != null) {
            throw new IllegalStateException(
                "Code '" + dto.getCode() + "' already exists in group '" + dto.getCodeGroup() + "'"
            );
        }

        codeMapper.insert(dto);
        return dto;
    }

    /**
     * 코드 수정
     */
    @Transactional
    public CodeDto updateCode(String id, CodeDto dto) {
        CodeDto existing = codeMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Code not found: " + id);
        }

        // 같은 그룹 내에서 code 중복 체크 (자기 자신 제외)
        CodeDto dup = codeMapper.findByGroupAndCode(existing.getCodeGroup(), dto.getCode());
        if (dup != null && !dup.getId().equals(id)) {
            throw new IllegalStateException(
                "Code '" + dto.getCode() + "' already exists in group '" + existing.getCodeGroup() + "'"
            );
        }

        dto.setId(id);
        dto.setCodeGroup(existing.getCodeGroup());
        if (dto.getCodeGroupName() == null) dto.setCodeGroupName(existing.getCodeGroupName());
        dto.setCreatedAt(existing.getCreatedAt());
        dto.setUpdatedAt(OffsetDateTime.now());
        codeMapper.update(dto);
        return dto;
    }

    /**
     * 코드 삭제
     */
    @Transactional
    public void deleteCode(String id) {
        CodeDto code = codeMapper.findById(id);
        if (code == null) {
            throw new NoSuchElementException("Code not found: " + id);
        }
        codeMapper.deleteById(id);
    }
}
