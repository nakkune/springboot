package com.nak.backend.item.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.item.dto.ItemDto;
import com.nak.backend.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 아이템 관리 REST API 컨트롤러
 */
@Tag(name = "아이템", description = "아이템 관리 API")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "게시판별 아이템 조회", description = "특정 게시판 ID에 속한 모든 아이템 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ItemDto>> getItemsByBoardId(@PathVariable String boardId) {
        return ResponseEntity.ok(itemService.getItemsByBoardId(boardId));
    }

    @Operation(summary = "그룹별 아이템 조회", description = "특정 그룹 ID에 속한 모든 아이템 목록을 조회합니다.")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ItemDto>> getItemsByGroupId(@PathVariable String groupId) {
        return ResponseEntity.ok(itemService.getItemsByGroupId(groupId));
    }

    @Operation(summary = "아이템 상세 조회", description = "특정 ID의 아이템 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable String id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @Operation(summary = "아이템 생성", description = "새로운 아이템을 생성합니다.")
    @PostMapping
    public ResponseEntity<ItemDto> createItem(@RequestBody ItemDto item) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(itemService.createItem(item));
    }

    @Operation(summary = "아이템 수정", description = "특정 ID의 아이템 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable String id,
            @RequestBody ItemDto item) {
        return ResponseEntity.ok(itemService.updateItem(id, item));
    }

    @Operation(summary = "아이템 삭제", description = "특정 ID의 아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
