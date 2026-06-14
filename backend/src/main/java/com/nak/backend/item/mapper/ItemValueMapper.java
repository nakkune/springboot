package com.nak.backend.item.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.item.dto.ItemValueDto;

/**
 * MyBatis ItemValue Mapper 인터페이스
 */
@Mapper
public interface ItemValueMapper {
    List<ItemValueDto> findAllByItemId(@Param("itemId") String itemId);
    // [NEW] 보드 전체의 아이템 밸류 데이터를 단 1회 쿼리로 고속 로드하기 위한 시니어 매퍼 설계
    List<ItemValueDto> findAllByBoardId(@Param("boardId") String boardId);
    ItemValueDto findByItemAndColumn(@Param("itemId") String itemId, @Param("columnId") String columnId);
    int insert(ItemValueDto itemValue);
    int update(ItemValueDto itemValue);
    int deleteById(@Param("id") String id);
}
