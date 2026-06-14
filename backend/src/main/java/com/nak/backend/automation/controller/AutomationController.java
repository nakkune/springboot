package com.nak.backend.automation.controller;

import com.nak.backend.automation.dto.AutomationDto;
import com.nak.backend.automation.service.AutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "자동화", description = "보드 내 작업 자동화 규칙 관리 API")
@RestController
@RequestMapping("/automations")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @Operation(summary = "자동화 목록 조회", description = "특정 보드의 자동화 규칙 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<AutomationDto>> getAutomations(@PathVariable String boardId) {
        return ResponseEntity.ok(automationService.getAutomationsByBoardId(boardId));
    }

    @Operation(summary = "자동화 생성", description = "새로운 자동화 규칙을 생성합니다.")
    @PostMapping
    public ResponseEntity<AutomationDto> createAutomation(@RequestBody AutomationDto automation) {
        return ResponseEntity.status(HttpStatus.CREATED).body(automationService.createAutomation(automation));
    }

    @Operation(summary = "자동화 수정", description = "기존 자동화 규칙을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<AutomationDto> updateAutomation(@PathVariable String id, @RequestBody AutomationDto automation) {
        return ResponseEntity.ok(automationService.updateAutomation(id, automation));
    }

    @Operation(summary = "자동화 삭제", description = "특정 자동화 규칙을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAutomation(@PathVariable String id) {
        automationService.deleteAutomation(id);
        return ResponseEntity.noContent().build();
    }
}

