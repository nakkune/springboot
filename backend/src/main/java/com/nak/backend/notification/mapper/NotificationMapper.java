package com.nak.backend.notification.mapper;

import com.nak.backend.notification.dto.NotificationDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface NotificationMapper {
    List<NotificationDto> findByRecipientId(String recipientId);
    void insertNotification(NotificationDto notification);
    void updateIsRead(String id, boolean isRead);
    int getUnreadCount(String recipientId);
}
