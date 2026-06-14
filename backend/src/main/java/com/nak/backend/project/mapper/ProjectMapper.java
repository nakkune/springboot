package com.nak.backend.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.project.dto.ProjectDto;

/**
 * MyBatis Project Mapper 인터페이스
 * 10년 차 시니어 관점에서 UUID-String 매핑 및 쿼리 메소드를 정밀 정의합니다.
 */
@Mapper
public interface ProjectMapper {
    List<ProjectDto> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);
    ProjectDto findById(@Param("id") String id);
    int insert(ProjectDto project);
    int update(ProjectDto project);
    int deleteById(@Param("id") String id);
}
