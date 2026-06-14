package com.nak.backend.workspace.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.user.dto.UserDto;
import com.nak.backend.user.mapper.UserMapper;
import com.nak.backend.workspace.dto.WorkspaceMemberDto;
import com.nak.backend.workspace.mapper.WorkspaceMemberMapper;
import lombok.RequiredArgsConstructor;

/**
 * 워크스페이스 멤버 관리 비즈니스 로직 서비스
 * 10년 차 시니어 설계: 자동 임시 가입 처리를 통한 온보딩 가이드를 지원합니다.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final UserMapper userMapper;

    /**
     * 특정 워크스페이스에 속한 멤버 리스트 조회
     */
    public List<WorkspaceMemberDto> getWorkspaceMembers(String workspaceId) {
        return workspaceMemberMapper.selectMembersByWorkspaceId(workspaceId);
    }

    /**
     * 특정 유저를 워크스페이스 멤버로 초대
     * 10년 차 시니어 설계: 초대받은 이메일의 유저가 존재하지 않으면, 신규 가입(임시 비밀번호 1234) 처리 후 안전하게 멤버로 추가합니다.
     */
    @Transactional
    public WorkspaceMemberDto inviteMember(String workspaceId, String email, String role) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
        }

        // 1. 이메일로 기존 유저 존재 체크
        UserDto user = userMapper.findByEmail(email.trim());
        
        if (user == null) {
            // 존재하지 않는 유저라면 온보딩을 위해 실시간 자동 가입 처리
            String newUserId = UUID.randomUUID().toString();
            String namePart = email.split("@")[0];
            // 첫 글자 대문자화한 이름 생성
            String fullName = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
            
            user = new UserDto();
            user.setId(newUserId);
            user.setEmail(email.trim());
            user.setPasswordHash("1234"); // 평문 폴백 매치 비밀번호 '1234'
            user.setFullName(fullName);
            user.setIsActive(true);
            
            userMapper.insert(user);
        }
        
        // 2. 이미 해당 워크스페이스에 가입되어 있는지 확인
        int existingCount = workspaceMemberMapper.countMember(workspaceId, user.getId());
        if (existingCount > 0) {
            throw new IllegalArgumentException("이미 해당 워크스페이스의 멤버로 등록된 사용자입니다.");
        }
        
        // 3. 관계 매핑 테이블에 추가
        String memberId = UUID.randomUUID().toString();
        workspaceMemberMapper.insertMember(memberId, workspaceId, user.getId(), role);
        
        // 4. 추가된 유저 정보 DTO 결과물 반환
        return new WorkspaceMemberDto(
            memberId,
            workspaceId,
            user.getId(),
            role,
            user.getEmail(),
            user.getFullName(),
            null
        );
    }

    /**
     * 특정 워크스페이스 멤버의 권한(Role) 수정
     */
    @Transactional
    public void updateMemberRole(String memberId, String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("올바른 역할을 입력해 주세요.");
        }
        workspaceMemberMapper.updateMemberRole(memberId, role.trim());
    }

    /**
     * 특정 워크스페이스 멤버 추방 (삭제)
     */
    @Transactional
    public void deleteMember(String memberId) {
        workspaceMemberMapper.deleteMember(memberId);
    }
}
