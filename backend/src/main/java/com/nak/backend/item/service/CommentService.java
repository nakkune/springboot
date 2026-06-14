package com.nak.backend.item.service;

import com.nak.backend.item.dto.CommentDto;
import com.nak.backend.item.mapper.CommentMapper;
import com.nak.backend.notification.dto.NotificationDto;
import com.nak.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    public List<CommentDto> getCommentsByItemId(String itemId) {
        return commentMapper.findByItemId(itemId);
    }

    @Transactional
    public CommentDto createComment(CommentDto comment) {
        // 1. 댓글 삽입
        commentMapper.insertComment(comment);
        
        // 2. 멘션 처리 및 실시간 알림 전송
        if (comment.getMentionedUserIds() != null && !comment.getMentionedUserIds().isEmpty()) {
            for (String mentionedUserId : comment.getMentionedUserIds()) {
                // DB에 멘션 기록
                commentMapper.insertCommentMention(comment.getId(), mentionedUserId);
                
                // 알림 생성 및 전송
                NotificationDto notification = new NotificationDto();
                notification.setRecipientId(mentionedUserId);
                notification.setSenderId(comment.getAuthorId());
                notification.setType("mention");
                notification.setTitle("새로운 멘션 알림");
                notification.setBody("님이 댓글에서 회원님을 멘션했습니다."); // UI에서 Sender 이름 조합
                notification.setRefType("comment");
                notification.setRefId(comment.getId());
                
                notificationService.sendNotification(notification);
            }
        }
        
        return commentMapper.findById(comment.getId());
    }

    public CommentDto updateComment(String id, CommentDto comment) {
        comment.setId(id);
        commentMapper.updateComment(comment);
        return commentMapper.findById(id);
    }

    public void deleteComment(String id) {
        commentMapper.deleteComment(id);
    }
}
