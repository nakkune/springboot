package com.nak.backend.board.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.board.dto.BoardGroupDto;
import com.nak.backend.board.mapper.BoardGroupMapper;
import lombok.RequiredArgsConstructor;

/**
 * 보드 그룹 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
public class BoardGroupService {

    private final BoardGroupMapper boardGroupMapper;

    public List<BoardGroupDto> getBoardGroupsByBoardId(String boardId) {
        return boardGroupMapper.findAllByBoardId(boardId);
    }

    public BoardGroupDto getBoardGroupById(String id) {
        BoardGroupDto boardGroup = boardGroupMapper.findById(id);
        if (boardGroup == null) {
            throw new IllegalArgumentException("보드 그룹을 찾을 수 없습니다. ID: " + id);
        }
        return boardGroup;
    }

    @Transactional
    public BoardGroupDto createBoardGroup(BoardGroupDto boardGroup) {
        if (boardGroup.getId() == null || boardGroup.getId().isEmpty()) {
            boardGroup.setId(UUID.randomUUID().toString());
        }
        if (boardGroup.getPosition() == null) boardGroup.setPosition(0);
        if (boardGroup.getIsCollapsed() == null) boardGroup.setIsCollapsed(false);

        boardGroupMapper.insert(boardGroup);
        return boardGroupMapper.findById(boardGroup.getId());
    }

    @Transactional
    public BoardGroupDto updateBoardGroup(String id, BoardGroupDto boardGroup) {
        BoardGroupDto existing = getBoardGroupById(id);
        
        if (boardGroup.getTitle() != null) existing.setTitle(boardGroup.getTitle());
        if (boardGroup.getColor() != null) existing.setColor(boardGroup.getColor());
        if (boardGroup.getPosition() != null) existing.setPosition(boardGroup.getPosition());
        if (boardGroup.getIsCollapsed() != null) existing.setIsCollapsed(boardGroup.getIsCollapsed());
        
        boardGroupMapper.update(existing);
        return boardGroupMapper.findById(id);
    }

    @Transactional
    public void deleteBoardGroup(String id) {
        getBoardGroupById(id);
        boardGroupMapper.deleteById(id);
    }
}
