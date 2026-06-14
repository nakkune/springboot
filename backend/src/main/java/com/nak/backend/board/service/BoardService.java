package com.nak.backend.board.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.board.dto.BoardDto;
import com.nak.backend.board.dto.BoardGroupDto;
import com.nak.backend.board.dto.BoardColumnDto;
import com.nak.backend.board.mapper.BoardMapper;
import com.nak.backend.board.mapper.BoardGroupMapper;
import com.nak.backend.board.mapper.BoardColumnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 보드 비즈니스 로직 처리 서비스
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;
    private final BoardGroupMapper boardGroupMapper;
    private final BoardColumnMapper boardColumnMapper;

    public List<BoardDto> getBoardsByProjectId(String projectId) {
        return boardMapper.findAllByProjectId(projectId);
    }

    public BoardDto getBoardById(String id) {
        BoardDto board = boardMapper.findById(id);
        if (board == null) {
            throw new IllegalArgumentException("보드를 찾을 수 없습니다. ID: " + id);
        }
        return board;
    }

    @Transactional
    public BoardDto createBoard(BoardDto board) {
        if (board.getId() == null || board.getId().isEmpty()) {
            board.setId(UUID.randomUUID().toString());
        }
        
        // SecurityContext에서 현재 로그인된 사용자 ID를 추출하여 강제 주입
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            board.setCreatedBy((String) authentication.getPrincipal());
        }

        if (board.getBoardType() == null) board.setBoardType("main");
        if (board.getPosition() == null) board.setPosition(0);
        if (board.getIsArchived() == null) board.setIsArchived(false);
        
        boardMapper.insert(board);
        
        // 10년 차 시니어 설계: 새 보드 생성 시 즉각 업무를 시작할 수 있도록 필수 4대 핵심 컬럼 기본 셋팅 (상태, 담당자, 마감일, 타임라인)
        boardColumnMapper.insert(new BoardColumnDto(UUID.randomUUID().toString(), board.getId(), "상태", "status", "{\"options\":[{\"id\":\"1\",\"label\":\"완료\",\"color\":\"#00C875\"},{\"id\":\"2\",\"label\":\"진행 중\",\"color\":\"#FDAB3D\"},{\"id\":\"3\",\"label\":\"시작 전\",\"color\":\"#C4C4C4\"},{\"id\":\"4\",\"label\":\"막힘\",\"color\":\"#E2445C\"}]}", 1, false, false, null, null));
        boardColumnMapper.insert(new BoardColumnDto(UUID.randomUUID().toString(), board.getId(), "담당자", "person", "{}", 2, false, false, null, null));
        boardColumnMapper.insert(new BoardColumnDto(UUID.randomUUID().toString(), board.getId(), "마감일", "date", "{}", 3, false, false, null, null));
        boardColumnMapper.insert(new BoardColumnDto(UUID.randomUUID().toString(), board.getId(), "타임라인", "timeline", "{}", 4, false, false, null, null));
        boardColumnMapper.insert(new BoardColumnDto(UUID.randomUUID().toString(), board.getId(), "우선순위", "priority", "{}", 5, false, false, null, null));

        // 필수 그룹 기본 셋팅 (10년 차 시니어 설계: 기본 '작업' 그룹 1개로 심플하게 시작)
        boardGroupMapper.insert(new BoardGroupDto(UUID.randomUUID().toString(), board.getId(), "작업", "#579BFC", 1, false, null, null));

        return boardMapper.findById(board.getId());
    }

    @Transactional
    public BoardDto updateBoard(String id, BoardDto board) {
        BoardDto existing = getBoardById(id);
        
        if (board.getName() != null) existing.setName(board.getName());
        if (board.getDescription() != null) existing.setDescription(board.getDescription());
        if (board.getBoardType() != null) existing.setBoardType(board.getBoardType());
        if (board.getPosition() != null) existing.setPosition(board.getPosition());
        if (board.getIsArchived() != null) existing.setIsArchived(board.getIsArchived());
        
        boardMapper.update(existing);
        return boardMapper.findById(id);
    }

    @Transactional
    public void deleteBoard(String id) {
        getBoardById(id);
        boardMapper.deleteById(id);
    }
}
