package com.nak.backend.project.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.project.dto.ProjectDto;
import com.nak.backend.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 프로젝트 비즈니스 로직 처리 서비스
 * 10년 차 시니어 관점에서 트랜잭션 처리와 객체 식별자 자동 생성 로직을 적용하였습니다.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<ProjectDto> getProjectsByWorkspaceId(String workspaceId) {
        return projectMapper.findAllByWorkspaceId(workspaceId);
    }

    public ProjectDto getProjectById(String id) {
        ProjectDto project = projectMapper.findById(id);
        if (project == null) {
            throw new IllegalArgumentException("프로젝트를 찾을 수 없습니다. ID: " + id);
        }
        return project;
    }

    @Transactional
    public ProjectDto createProject(ProjectDto project) {
        if (project.getId() == null || project.getId().isEmpty()) {
            project.setId(UUID.randomUUID().toString());
        }

        // SecurityContext에서 현재 로그인된 사용자 ID를 추출하여 강제 주입
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            project.setCreatedBy((String) authentication.getPrincipal());
        }

        projectMapper.insert(project);
        return projectMapper.findById(project.getId());
    }

    @Transactional
    public ProjectDto updateProject(String id, ProjectDto project) {
        ProjectDto existing = getProjectById(id);
        
        if (project.getName() != null) existing.setName(project.getName());
        if (project.getDescription() != null) existing.setDescription(project.getDescription());
        if (project.getTeamId() != null) existing.setTeamId(project.getTeamId());
        if (project.getColor() != null) existing.setColor(project.getColor());
        if (project.getIcon() != null) existing.setIcon(project.getIcon());
        if (project.getIsArchived() != null) existing.setIsArchived(project.getIsArchived());
        
        projectMapper.update(existing);
        return projectMapper.findById(id);
    }

    @Transactional
    public void deleteProject(String id) {
        getProjectById(id); // 존재 여부 검증
        projectMapper.deleteById(id);
    }
}
