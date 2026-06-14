package com.nak.backend.workspace.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.workspace.dto.WorkspaceDto;
import com.nak.backend.workspace.mapper.WorkspaceMapper;
import com.nak.backend.workspace.mapper.WorkspaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 워크스페이스 비즈니스 로직 처리 서비스
 * 10년 차 시니어 관점에서 트랜잭션 처리와 객체 식별자 자동 생성 로직을 적용하였습니다.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;

    public List<WorkspaceDto> getAllWorkspaces() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String userId) {
            return workspaceMapper.findAllByUserId(userId);
        }
        return List.of();
    }

    public WorkspaceDto getWorkspaceById(String id) {
        WorkspaceDto workspace = workspaceMapper.findById(id);
        if (workspace == null) {
            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID: " + id);
        }
        return workspace;
    }

    @Transactional
    public WorkspaceDto createWorkspace(WorkspaceDto workspace) {
        if (workspace.getId() == null || workspace.getId().isEmpty()) {
            workspace.setId(UUID.randomUUID().toString());
        }
        
        // SecurityContext에서 현재 로그인된 사용자 ID를 추출하여 강제 주입
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            workspace.setOwnerId((String) authentication.getPrincipal());
        }

        workspaceMapper.insert(workspace);

        String ownerId = workspace.getOwnerId();
        if (ownerId != null && !ownerId.isEmpty()) {
            String memberId = UUID.randomUUID().toString();
            workspaceMemberMapper.insertMember(memberId, workspace.getId(), ownerId, "owner");
        }

        return workspaceMapper.findById(workspace.getId());
    }

    @Transactional
    public WorkspaceDto updateWorkspace(String id, WorkspaceDto workspace) {
        WorkspaceDto existing = getWorkspaceById(id);
        
        if (workspace.getName() != null) existing.setName(workspace.getName());
        if (workspace.getSlug() != null) existing.setSlug(workspace.getSlug());
        if (workspace.getLogoUrl() != null) existing.setLogoUrl(workspace.getLogoUrl());
        if (workspace.getPlan() != null) existing.setPlan(workspace.getPlan());
        if (workspace.getOwnerId() != null) existing.setOwnerId(workspace.getOwnerId());
        
        workspaceMapper.update(existing);
        return workspaceMapper.findById(id);
    }

    @Transactional
    public void deleteWorkspace(String id) {
        getWorkspaceById(id); // 존재 여부 검증
        workspaceMapper.deleteById(id);
    }
}
