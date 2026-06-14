package com.nak.backend.project.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.project.dto.ProjectDto;
import com.nak.backend.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 관리 REST API 컨트롤러
 * 10년 차 시니어 관점에서 RESTful 규격을 준수하여 안전하게 구동되도록 작성했습니다.
 */
@Tag(name = "프로젝트 관리", description = "프로젝트를 관리하는 API")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "워크스페이스별 프로젝트 목록 조회", description = "특정 워크스페이스 ID에 해당하는 모든 프로젝트 목록을 조회합니다.")
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<ProjectDto>> getProjectsByWorkspaceId(@PathVariable String workspaceId) {
        return ResponseEntity.ok(projectService.getProjectsByWorkspaceId(workspaceId));
    }

    @Operation(summary = "프로젝트 상세 조회", description = "프로젝트 ID로 특정 프로젝트를 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다.")
    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody ProjectDto project) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(projectService.createProject(project));
    }

    @Operation(summary = "프로젝트 수정", description = "기존 프로젝트 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable String id,
            @RequestBody ProjectDto project) {
        return ResponseEntity.ok(projectService.updateProject(id, project));
    }

    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
