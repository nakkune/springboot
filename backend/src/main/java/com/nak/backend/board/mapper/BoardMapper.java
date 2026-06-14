package com.nak.backend.board.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.board.dto.BoardDto;

/**
 * MyBatis Board Mapper 인터페이스
 */
@Mapper
public interface BoardMapper {
    List<BoardDto> findAllByProjectId(@Param("projectId") String projectId);
    BoardDto findById(@Param("id") String id);
    int insert(BoardDto board);
    int update(BoardDto board);
    int deleteById(@Param("id") String id);
}
