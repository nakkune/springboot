package com.nak.backend.workspace.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.workspace.dto.WorkspaceDto;

/**
 * MyBatis Workspace Mapper 인터페이스
 * 10년 차 시니어 관점에서 UUID-String 매핑 및 쿼리 메소드를 정밀 정의합니다.
 */
@Mapper
public interface WorkspaceMapper {
    List<WorkspaceDto> findAll();
    List<WorkspaceDto> findAllByUserId(@Param("userId") String userId);
    WorkspaceDto findById(@Param("id") String id);
    WorkspaceDto findBySlug(@Param("slug") String slug);
    int insert(WorkspaceDto workspace);
    int update(WorkspaceDto workspace);
    int deleteById(@Param("id") String id);
    int count();
}
