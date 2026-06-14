package com.nak.backend.board.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.board.dto.BoardColumnDto;
import com.nak.backend.board.mapper.BoardColumnMapper;
import lombok.RequiredArgsConstructor;

/**
 * 보드 컬럼 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
public class BoardColumnService {

    private final BoardColumnMapper boardColumnMapper;

    public List<BoardColumnDto> getBoardColumnsByBoardId(String boardId) {
        return boardColumnMapper.findAllByBoardId(boardId);
    }

    public BoardColumnDto getBoardColumnById(String id) {
        BoardColumnDto boardColumn = boardColumnMapper.findById(id);
        if (boardColumn == null) {
            throw new IllegalArgumentException("보드 컬럼을 찾을 수 없습니다. ID: " + id);
        }
        return boardColumn;
    }

    @Transactional
    public BoardColumnDto createBoardColumn(BoardColumnDto boardColumn) {
        if (boardColumn.getId() == null || boardColumn.getId().isEmpty()) {
            boardColumn.setId(UUID.randomUUID().toString());
        }
        if (boardColumn.getSettings() == null) boardColumn.setSettings("{}");
        if (boardColumn.getPosition() == null) boardColumn.setPosition(0);
        if (boardColumn.getIsRequired() == null) boardColumn.setIsRequired(false);
        if (boardColumn.getIsHidden() == null) boardColumn.setIsHidden(false);

        boardColumnMapper.insert(boardColumn);
        return boardColumnMapper.findById(boardColumn.getId());
    }

    @Transactional
    public BoardColumnDto updateBoardColumn(String id, BoardColumnDto boardColumn) {
        BoardColumnDto existing = getBoardColumnById(id);
        
        if (boardColumn.getName() != null) existing.setName(boardColumn.getName());
        if (boardColumn.getColumnType() != null) existing.setColumnType(boardColumn.getColumnType());
        if (boardColumn.getSettings() != null) existing.setSettings(boardColumn.getSettings());
        if (boardColumn.getPosition() != null) existing.setPosition(boardColumn.getPosition());
        if (boardColumn.getIsRequired() != null) existing.setIsRequired(boardColumn.getIsRequired());
        if (boardColumn.getIsHidden() != null) existing.setIsHidden(boardColumn.getIsHidden());
        
        boardColumnMapper.update(existing);
        return boardColumnMapper.findById(id);
    }

    @Transactional
    public void deleteBoardColumn(String id) {
        getBoardColumnById(id);
        boardColumnMapper.deleteById(id);
    }
}
