package com.nak.backend.board.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.board.dto.BoardColumnDto;

/**
 * MyBatis Board Column Mapper 인터페이스
 */
@Mapper
public interface BoardColumnMapper {
    List<BoardColumnDto> findAllByBoardId(@Param("boardId") String boardId);
    BoardColumnDto findById(@Param("id") String id);
    int insert(BoardColumnDto boardColumn);
    int update(BoardColumnDto boardColumn);
    int deleteById(@Param("id") String id);
}
