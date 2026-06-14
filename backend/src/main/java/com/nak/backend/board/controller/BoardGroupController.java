package com.nak.backend.board.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.board.dto.BoardGroupDto;
import com.nak.backend.board.service.BoardGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 보드 그룹 관리 REST API 컨트롤러
 */
@Tag(name = "보드 그룹 관리", description = "보드 그룹을 관리하는 API")
@RestController
@RequestMapping("/board-groups")
@RequiredArgsConstructor
public class BoardGroupController {

    private final BoardGroupService boardGroupService;

    @Operation(summary = "보드별 그룹 목록 조회", description = "특정 보드 ID에 해당하는 모든 보드 그룹 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<BoardGroupDto>> getBoardGroupsByBoardId(@PathVariable String boardId) {
        return ResponseEntity.ok(boardGroupService.getBoardGroupsByBoardId(boardId));
    }

    @Operation(summary = "보드 그룹 상세 조회", description = "보드 그룹 ID로 특정 보드 그룹을 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<BoardGroupDto> getBoardGroupById(@PathVariable String id) {
        return ResponseEntity.ok(boardGroupService.getBoardGroupById(id));
    }

    @Operation(summary = "보드 그룹 생성", description = "새로운 보드 그룹을 생성합니다.")
    @PostMapping
    public ResponseEntity<BoardGroupDto> createBoardGroup(@RequestBody BoardGroupDto boardGroup) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(boardGroupService.createBoardGroup(boardGroup));
    }

    @Operation(summary = "보드 그룹 수정", description = "기존 보드 그룹 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<BoardGroupDto> updateBoardGroup(
            @PathVariable String id,
            @RequestBody BoardGroupDto boardGroup) {
        return ResponseEntity.ok(boardGroupService.updateBoardGroup(id, boardGroup));
    }

    @Operation(summary = "보드 그룹 삭제", description = "특정 보드 그룹을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoardGroup(@PathVariable String id) {
        boardGroupService.deleteBoardGroup(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
