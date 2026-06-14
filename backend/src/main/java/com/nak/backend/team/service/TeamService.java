package com.nak.backend.team.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.team.dto.TeamDto;
import com.nak.backend.team.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;

/**
 * 팀 비즈니스 로직 처리 서비스
 * 10년 차 시니어 관점에서 트랜잭션 처리와 객체 식별자 자동 생성 로직을 적용하였습니다.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamMapper teamMapper;

    public List<TeamDto> getTeamsByWorkspaceId(String workspaceId) {
        return teamMapper.findAllByWorkspaceId(workspaceId);
    }

    public TeamDto getTeamById(String id) {
        TeamDto team = teamMapper.findById(id);
        if (team == null) {
            throw new IllegalArgumentException("팀을 찾을 수 없습니다. ID: " + id);
        }
        return team;
    }

    @Transactional
    public TeamDto createTeam(TeamDto team) {
        if (team.getId() == null || team.getId().isEmpty()) {
            team.setId(UUID.randomUUID().toString());
        }
        teamMapper.insert(team);
        return teamMapper.findById(team.getId());
    }

    @Transactional
    public TeamDto updateTeam(String id, TeamDto team) {
        TeamDto existing = getTeamById(id);
        
        if (team.getName() != null) existing.setName(team.getName());
        if (team.getDescription() != null) existing.setDescription(team.getDescription());
        if (team.getColor() != null) existing.setColor(team.getColor());
        
        teamMapper.update(existing);
        return teamMapper.findById(id);
    }

    @Transactional
    public void deleteTeam(String id) {
        getTeamById(id); // 존재 여부 검증
        teamMapper.deleteById(id);
    }
}
