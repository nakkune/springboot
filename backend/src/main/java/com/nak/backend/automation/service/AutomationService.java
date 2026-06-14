package com.nak.backend.automation.service;

import com.nak.backend.automation.dto.AutomationDto;
import com.nak.backend.automation.mapper.AutomationMapper;
import com.nak.backend.notification.dto.NotificationDto;
import com.nak.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final AutomationMapper automationMapper;
    private final NotificationService notificationService;

    public List<AutomationDto> getAutomationsByBoardId(String boardId) {
        return automationMapper.findByBoardId(boardId);
    }

    public AutomationDto createAutomation(AutomationDto automation) {
        automationMapper.insertAutomation(automation);
        return automationMapper.findById(automation.getId());
    }

    public AutomationDto updateAutomation(String id, AutomationDto automation) {
        automation.setId(id);
        automationMapper.updateAutomation(automation);
        return automationMapper.findById(id);
    }

    public void deleteAutomation(String id) {
        automationMapper.deleteAutomation(id);
    }

    /**
     * 아이템 변경 등 이벤트 발생 시 호출되는 자동화 실행기
     * (향후 EventListener 또는 Queue를 통해 비동기로 호출 가능)
     */
    public void evaluateAndExecute(String boardId, String eventType, String columnId, String newValue, String triggerUserId) {
        List<AutomationDto> automations = automationMapper.findByBoardId(boardId);
        
        for (AutomationDto auto : automations) {
            if (!auto.getIsActive()) continue;
            
            // 단순 구현: Trigger JSON 파싱 대신 문자열 포함 여부 검사 (실제 환경에서는 Jackson 파싱 필요)
            // 예: {"type":"status_change", "column_id":"col123", "to_value":"Done"}
            String trigger = auto.getTriggerConfig();
            if (trigger != null && trigger.contains(eventType) && trigger.contains(columnId) && trigger.contains(newValue)) {
                
                log.info("Automation triggered: {}", auto.getName());
                
                // 실행 횟수 증가
                automationMapper.updateRunStatus(auto.getId());
                
                // Action 실행 로직
                String action = auto.getActionConfig();
                if (action != null && action.contains("notify")) {
                    // 예: "notify", "target":"admin"
                    NotificationDto notification = new NotificationDto();
                    notification.setRecipientId(triggerUserId); // 단순 알림용
                    notification.setType("automation");
                    notification.setTitle("자동화 실행 알림: " + auto.getName());
                    notification.setBody("자동화 규칙에 의해 알림이 발생했습니다.");
                    notification.setRefType("board");
                    notification.setRefId(boardId);
                    
                    notificationService.sendNotification(notification);
                }
            }
        }
    }
}
