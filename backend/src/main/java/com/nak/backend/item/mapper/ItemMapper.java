package com.nak.backend.item.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.item.dto.ItemDto;

/**
 * MyBatis Item Mapper 인터페이스
 */
@Mapper
public interface ItemMapper {
    List<ItemDto> findAllByBoardId(@Param("boardId") String boardId);
    List<ItemDto> findAllByGroupId(@Param("groupId") String groupId);
    List<ItemDto> findAllByParentItemId(@Param("parentItemId") String parentItemId);
    ItemDto findById(@Param("id") String id);
    int insert(ItemDto item);
    int update(ItemDto item);
    int deleteById(@Param("id") String id);
}
