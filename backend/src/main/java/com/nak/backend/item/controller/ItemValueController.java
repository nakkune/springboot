package com.nak.backend.item.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.item.dto.ItemValueDto;
import com.nak.backend.item.service.ItemValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 아이템 밸류 관리 REST API 컨트롤러
 */
@Tag(name = "아이템 밸류", description = "아이템 밸류(속성 값) 관리 API")
@RestController
@RequestMapping("/item-values")
@RequiredArgsConstructor
public class ItemValueController {

    private final ItemValueService itemValueService;

    @Operation(summary = "아이템별 밸류 조회", description = "특정 아이템 ID에 해당하는 모든 아이템 밸류 목록을 조회합니다.")
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<ItemValueDto>> getItemValuesByItemId(@PathVariable String itemId) {
        return ResponseEntity.ok(itemValueService.getItemValuesByItemId(itemId));
    }

    @Operation(summary = "게시판별 밸류 조회", description = "특정 게시판 ID에 속한 모든 아이템 밸류 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ItemValueDto>> getItemValuesByBoardId(@PathVariable String boardId) {
        return ResponseEntity.ok(itemValueService.getItemValuesByBoardId(boardId));
    }

    @Operation(summary = "아이템 및 컬럼별 밸류 상세 조회", description = "특정 아이템 ID와 컬럼 ID에 매칭되는 아이템 밸류의 상세 정보를 조회합니다.")
    @GetMapping("/item/{itemId}/column/{columnId}")
    public ResponseEntity<ItemValueDto> getItemValueByItemAndColumn(
            @PathVariable String itemId, @PathVariable String columnId) {
        ItemValueDto value = itemValueService.getItemValueByItemAndColumn(itemId, columnId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }

    @Operation(summary = "아이템 밸류 저장", description = "새로운 아이템 밸류를 생성하거나 기존 밸류를 갱신하여 저장합니다.")
    @PostMapping
    public ResponseEntity<ItemValueDto> saveItemValue(@RequestBody ItemValueDto itemValue) {
        return ResponseEntity.ok(itemValueService.saveItemValue(itemValue));
    }

    // [NEW] 10년 차 시니어 설계: 다차원 EAV 값을 단일 패킷으로 한 번에 고속 저장하기 위한 벌크 API 신설
    @Operation(summary = "아이템 밸류 벌크 저장", description = "여러 개의 아이템 밸류를 일괄로 저장합니다.")
    @PostMapping("/bulk")
    public ResponseEntity<List<ItemValueDto>> saveItemValuesBulk(@RequestBody List<ItemValueDto> itemValues) {
        return ResponseEntity.ok(itemValueService.saveItemValuesBulk(itemValues));
    }

    @Operation(summary = "아이템 밸류 삭제", description = "특정 ID의 아이템 밸류를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItemValue(@PathVariable String id) {
        itemValueService.deleteItemValue(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
