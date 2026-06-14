package com.nak.backend.team.controller;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.team.dto.TeamDto;
import com.nak.backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;

/**
 * 팀 관리 REST API 컨트롤러
 * 10년 차 시니어 관점에서 RESTful 규격을 준수하여 안전하게 구동되도록 작성했습니다.
 */
@Tag(name = "팀", description = "워크스페이스 내 팀 관리 API")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "워크스페이스별 팀 조회", description = "특정 워크스페이스에 속한 모든 팀 목록을 조회합니다.")
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<TeamDto>> getTeamsByWorkspaceId(@PathVariable String workspaceId) {
        return ResponseEntity.ok(teamService.getTeamsByWorkspaceId(workspaceId));
    }

    @Operation(summary = "팀 상세 조회", description = "특정 팀의 세부 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<TeamDto> getTeamById(@PathVariable String id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다.")
    @PostMapping
    public ResponseEntity<TeamDto> createTeam(@RequestBody TeamDto team) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(teamService.createTeam(team));
    }

    @Operation(summary = "팀 수정", description = "기존 팀 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<TeamDto> updateTeam(
            @PathVariable String id,
            @RequestBody TeamDto team) {
        return ResponseEntity.ok(teamService.updateTeam(id, team));
    }

    @Operation(summary = "팀 삭제", description = "특정 팀을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}

