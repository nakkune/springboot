package com.nak.backend.erp.hr.controller;

import com.nak.backend.erp.hr.dto.CodeDto;
import com.nak.backend.erp.hr.dto.CodeGroupDto;
import com.nak.backend.erp.hr.service.CodeService;
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
 * ERP 공통 코드 Controller
 * 기준정보(직위, 고용형태, 휴가유형 등) CRUD REST API를 제공합니다.
 */
@Tag(name = "공통 코드 (Common Code)", description = "ERP 기준 정보 - 공통 코드 그룹 및 하위 코드 목록 CRUD API를 제공합니다.")
@RestController
@RequestMapping("/erp/hr/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    /**
     * 코드 그룹 목록 조회 (식별자 + 표시명)
     */
    @Operation(summary = "공통 코드 그룹 목록 조회", description = "등록된 모든 공통 코드 그룹(Position, Job Title, Leave Type 등)의 식별자와 표시명을 조회합니다.")
    @GetMapping("/groups")
    public ResponseEntity<List<CodeGroupDto>> getGroups() {
        return ResponseEntity.ok(codeService.getGroups());
    }

    /**
     * 특정 그룹의 코드 목록 조회
     */
    @Operation(summary = "특정 그룹의 코드 목록 조회", description = "요청인자 group에 해당하는 특정 코드 그룹의 하위 코드 목록을 정렬 순서대로 조회합니다. group이 없으면 전체 코드를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CodeDto>> getCodes(
            @RequestParam(required = false) String group) {
        if (group != null) {
            return ResponseEntity.ok(codeService.getCodes(group));
        }
        return ResponseEntity.ok(codeService.getAllCodes());
    }

    /**
     * 코드 단건 조회
     */
    @Operation(summary = "코드 단건 조회", description = "코드 ID를 기준으로 개별 공통코드의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<CodeDto> getCode(@PathVariable String id) {
        return ResponseEntity.ok(codeService.getCode(id));
    }

    /**
     * 코드 생성
     */
    @Operation(summary = "공통 코드 신규 생성", description = "새로운 기준정보 공통코드를 추가 등록합니다.")
    @PostMapping
    public ResponseEntity<CodeDto> createCode(@RequestBody CodeDto dto) {
        CodeDto created = codeService.createCode(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 코드 수정
     */
    @Operation(summary = "공통 코드 수정", description = "코드 ID를 기준으로 기존 등록된 공통코드의 라벨명, 정렬 순서, 활성화 여부 등을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<CodeDto> updateCode(@PathVariable String id, @RequestBody CodeDto dto) {
        return ResponseEntity.ok(codeService.updateCode(id, dto));
    }

    /**
     * 코드 삭제
     */
    @Operation(summary = "공통 코드 삭제", description = "코드 ID를 기준으로 해당 공통코드 항목을 완전히 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCode(@PathVariable String id) {
        codeService.deleteCode(id);
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
