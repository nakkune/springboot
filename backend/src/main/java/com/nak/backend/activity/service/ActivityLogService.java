package com.nak.backend.activity.service;

import com.nak.backend.activity.dto.ActivityLogDto;
import com.nak.backend.activity.mapper.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    public List<ActivityLogDto> getActivityLogsByItemId(String itemId) {
        return activityLogMapper.findByItemId(itemId);
    }

    public void logActivity(String itemId, String actorId, String action, String meta) {
        // Find boardId, projectId, workspaceId from itemId if needed, 
        // but for simplicity we will just log with itemId for now.
        ActivityLogDto log = new ActivityLogDto();
        log.setItemId(itemId);
        log.setActorId(actorId);
        log.setAction(action);
        log.setMeta(meta);
        activityLogMapper.insertActivityLog(log);
    }
}
