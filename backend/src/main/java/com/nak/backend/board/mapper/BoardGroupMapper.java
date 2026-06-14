package com.nak.backend.board.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.board.dto.BoardGroupDto;

/**
 * MyBatis Board Group Mapper 인터페이스
 */
@Mapper
public interface BoardGroupMapper {
    List<BoardGroupDto> findAllByBoardId(@Param("boardId") String boardId);
    BoardGroupDto findById(@Param("id") String id);
    int insert(BoardGroupDto boardGroup);
    int update(BoardGroupDto boardGroup);
    int deleteById(@Param("id") String id);
}
