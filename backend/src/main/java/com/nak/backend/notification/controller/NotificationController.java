package com.nak.backend.notification.controller;

import com.nak.backend.notification.dto.NotificationDto;
import com.nak.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "알림", description = "실시간 알림 구독 및 관리 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 클라이언트 SSE 구독 엔드포인트
     */
    @Operation(summary = "실시간 알림 구독", description = "SSE(Server-Sent Events)를 통해 실시간 알림을 구독합니다.")
    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable String userId,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(userId, lastEventId);
    }

    /**
     * 알림 목록 조회
     */
    @Operation(summary = "알림 목록 조회", description = "특정 사용자의 전체 알림 목록을 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    /**
     * 안 읽은 알림 개수 조회
     */
    @Operation(summary = "안 읽은 알림 개수 조회", description = "특정 사용자의 아직 읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    /**
     * 알림 읽음 처리
     */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}

