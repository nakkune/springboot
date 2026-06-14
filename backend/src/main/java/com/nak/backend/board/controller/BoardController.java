package com.nak.backend.board.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.board.dto.BoardDto;
import com.nak.backend.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 보드 관리 REST API 컨트롤러
 */
@Tag(name = "보드 관리", description = "보드를 관리하는 API")
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "프로젝트별 보드 목록 조회", description = "특정 프로젝트 ID에 해당하는 모든 보드 목록을 조회합니다.")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<BoardDto>> getBoardsByProjectId(@PathVariable String projectId) {
        return ResponseEntity.ok(boardService.getBoardsByProjectId(projectId));
    }

    @Operation(summary = "보드 상세 조회", description = "보드 ID로 특정 보드를 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardById(@PathVariable String id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    @Operation(summary = "보드 생성", description = "새로운 보드를 생성합니다.")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@RequestBody BoardDto board) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(boardService.createBoard(board));
    }

    @Operation(summary = "보드 수정", description = "기존 보드 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<BoardDto> updateBoard(
            @PathVariable String id,
            @RequestBody BoardDto board) {
        return ResponseEntity.ok(boardService.updateBoard(id, board));
    }

    @Operation(summary = "보드 삭제", description = "특정 보드를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
