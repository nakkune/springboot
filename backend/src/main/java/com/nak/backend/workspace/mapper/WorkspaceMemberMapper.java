package com.nak.backend.workspace.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.workspace.dto.WorkspaceMemberDto;

/**
 * 워크스페이스 멤버 데이터 처리 매퍼 인터페이스
 */
@Mapper
public interface WorkspaceMemberMapper {

    /**
     * 특정 워크스페이스에 소속된 멤버 목록 조회 (사용자 정보 조인)
     */
    List<WorkspaceMemberDto> selectMembersByWorkspaceId(@Param("workspaceId") String workspaceId);

    /**
     * 신규 멤버 추가
     */
    void insertMember(
        @Param("id") String id,
        @Param("workspaceId") String workspaceId,
        @Param("userId") String userId,
        @Param("role") String role
    );

    /**
     * 특정 사용자가 해당 워크스페이스의 멤버로 이미 등록되어 있는지 확인
     */
    int countMember(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 멤버 역할(Role) 수정
     */
    void updateMemberRole(@Param("id") String id, @Param("role") String role);

    /**
     * 멤버 삭제 (추방)
     */
    void deleteMember(@Param("id") String id);
}
