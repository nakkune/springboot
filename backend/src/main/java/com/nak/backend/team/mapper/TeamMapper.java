package com.nak.backend.team.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.team.dto.TeamDto;

/**
 * MyBatis Team Mapper 인터페이스
 * 10년 차 시니어 관점에서 UUID-String 매핑 및 쿼리 메소드를 정밀 정의합니다.
 */
@Mapper
public interface TeamMapper {
    List<TeamDto> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);
    TeamDto findById(@Param("id") String id);
    int insert(TeamDto team);
    int update(TeamDto team);
    int deleteById(@Param("id") String id);
}
