package com.nak.backend.notification.service;

import com.nak.backend.notification.dto.NotificationDto;
import com.nak.backend.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    
    // 유저 ID별 다중 세션을 지원하기 위해 ConcurrentHashMap 사용 (예: 멀티 브라우저 탭)
    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 구독
     */
    public SseEmitter subscribe(String userId, String lastEventId) {
        // SSE 타임아웃 1시간으로 설정
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        
        emitters.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitterId));
        emitter.onTimeout(() -> removeEmitter(userId, emitterId));
        emitter.onError((e) -> removeEmitter(userId, emitterId));

        // 연결 확인용 더미 이벤트 발송 (503 방지)
        sendToClient(userId, emitterId, emitter, "connect", "Connected!");

        return emitter;
    }

    private void removeEmitter(String userId, String emitterId) {
        Map<String, SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitterId);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    private void sendToClient(String userId, String emitterId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().id(String.valueOf(System.currentTimeMillis())).name(eventName).data(data));
        } catch (IOException e) {
            removeEmitter(userId, emitterId);
            log.warn("Failed to send SSE event to user {}", userId, e);
        }
    }

    /**
     * 실시간 알림 발송 및 DB 저장
     */
    public void sendNotification(NotificationDto notification) {
        // 1. DB에 알림 저장
        notificationMapper.insertNotification(notification);
        
        // 2. 현재 접속 중인 유저에게 SSE 푸시
        String recipientId = notification.getRecipientId();
        Map<String, SseEmitter> userEmitters = emitters.get(recipientId);
        
        if (userEmitters != null) {
            userEmitters.forEach((emitterId, emitter) -> {
                sendToClient(recipientId, emitterId, emitter, "notification", notification);
            });
        }
    }

    public List<NotificationDto> getNotifications(String userId) {
        return notificationMapper.findByRecipientId(userId);
    }

    public void markAsRead(String notificationId) {
        notificationMapper.updateIsRead(notificationId, true);
    }
    
    public int getUnreadCount(String userId) {
        return notificationMapper.getUnreadCount(userId);
    }
}
