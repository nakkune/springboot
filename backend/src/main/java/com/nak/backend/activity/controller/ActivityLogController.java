package com.nak.backend.activity.controller;

import com.nak.backend.activity.dto.ActivityLogDto;
import com.nak.backend.activity.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "활동 로그", description = "아이템 활동 로그 조회 API")
@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @Operation(summary = "아이템별 활동 로그 조회", description = "특정 아이템에 대한 활동 로그 목록을 조회합니다.")
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<ActivityLogDto>> getActivityLogsByItemId(@PathVariable String itemId) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByItemId(itemId));
    }
}

