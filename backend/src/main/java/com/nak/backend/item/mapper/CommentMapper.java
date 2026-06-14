package com.nak.backend.item.mapper;

import com.nak.backend.item.dto.CommentDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<CommentDto> findByItemId(String itemId);
    CommentDto findById(String id);
    void insertComment(CommentDto comment);
    void updateComment(CommentDto comment);
    void deleteComment(String id);
    
    // 멘션 저장
    void insertCommentMention(String commentId, String mentionedUserId);
}
