package com.nak.backend.board.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.board.dto.BoardColumnDto;
import com.nak.backend.board.service.BoardColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 보드 컬럼 관리 REST API 컨트롤러
 */
@Tag(name = "보드 컬럼 관리", description = "보드 컬럼을 관리하는 API")
@RestController
@RequestMapping("/board-columns")
@RequiredArgsConstructor
public class BoardColumnController {

    private final BoardColumnService boardColumnService;

    @Operation(summary = "보드별 컬럼 목록 조회", description = "특정 보드 ID에 해당하는 모든 보드 컬럼 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<BoardColumnDto>> getBoardColumnsByBoardId(@PathVariable String boardId) {
        return ResponseEntity.ok(boardColumnService.getBoardColumnsByBoardId(boardId));
    }

    @Operation(summary = "보드 컬럼 상세 조회", description = "보드 컬럼 ID로 특정 보드 컬럼을 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<BoardColumnDto> getBoardColumnById(@PathVariable String id) {
        return ResponseEntity.ok(boardColumnService.getBoardColumnById(id));
    }

    @Operation(summary = "보드 컬럼 생성", description = "새로운 보드 컬럼을 생성합니다.")
    @PostMapping
    public ResponseEntity<BoardColumnDto> createBoardColumn(@RequestBody BoardColumnDto boardColumn) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(boardColumnService.createBoardColumn(boardColumn));
    }

    @Operation(summary = "보드 컬럼 수정", description = "기존 보드 컬럼 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<BoardColumnDto> updateBoardColumn(
            @PathVariable String id,
            @RequestBody BoardColumnDto boardColumn) {
        return ResponseEntity.ok(boardColumnService.updateBoardColumn(id, boardColumn));
    }

    @Operation(summary = "보드 컬럼 삭제", description = "특정 보드 컬럼을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoardColumn(@PathVariable String id) {
        boardColumnService.deleteBoardColumn(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
