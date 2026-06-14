package com.nak.backend.workspace.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.workspace.dto.WorkspaceDto;
import com.nak.backend.workspace.dto.WorkspaceMemberDto; // [NEW] 멤버 DTO 임포트
import com.nak.backend.workspace.service.WorkspaceService;
import com.nak.backend.workspace.service.WorkspaceMemberService; // [NEW] 멤버 서비스 임포트
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 워크스페이스 관리 REST API 컨트롤러
 * 10년 차 시니어 관점에서 RESTful 규격을 준수하여 안전하게 구동되도록 작성했습니다.
 */
@Tag(name = "워크스페이스 관리", description = "워크스페이스를 관리하는 API")
@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService workspaceMemberService; // [NEW] 멤버 서비스 의존성 주입

    @Operation(summary = "워크스페이스 전체 목록 조회", description = "시스템 내 모든 워크스페이스 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<WorkspaceDto>> getAllWorkspaces() {
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }

    @Operation(summary = "워크스페이스 상세 조회", description = "워크스페이스 ID로 특정 워크스페이스를 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceDto> getWorkspaceById(@PathVariable String id) {
        return ResponseEntity.ok(workspaceService.getWorkspaceById(id));
    }

    @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스를 생성합니다.")
    @PostMapping
    public ResponseEntity<WorkspaceDto> createWorkspace(@RequestBody WorkspaceDto workspace) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(workspaceService.createWorkspace(workspace));
    }

    @Operation(summary = "워크스페이스 수정", description = "기존 워크스페이스 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceDto> updateWorkspace(
            @PathVariable String id,
            @RequestBody WorkspaceDto workspace) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, workspace));
    }

    @Operation(summary = "워크스페이스 삭제", description = "특정 워크스페이스를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable String id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // [NEW] 워크스페이스 멤버(팀원) 관리 API 엔드포인트
    // ==========================================

    @Operation(summary = "워크스페이스 멤버 목록 조회", description = "특정 워크스페이스에 소속된 멤버(팀원) 목록을 조회합니다.")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberDto>> getWorkspaceMembers(@PathVariable String id) {
        return ResponseEntity.ok(workspaceMemberService.getWorkspaceMembers(id));
    }

    @Operation(summary = "워크스페이스 멤버 초대", description = "이메일을 사용하여 워크스페이스에 새로운 멤버를 초대합니다.")
    @PostMapping("/{id}/members/invite")
    public ResponseEntity<WorkspaceMemberDto> inviteMember(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String role = request.getOrDefault("role", "member");
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(workspaceMemberService.inviteMember(id, email, role));
     }

    @Operation(summary = "워크스페이스 멤버 역할 수정", description = "워크스페이스 내 특정 멤버의 역할을 수정합니다.")
    @PutMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable String workspaceId,
            @PathVariable String memberId,
            @RequestBody Map<String, String> request) {
        String role = request.get("role");
        workspaceMemberService.updateMemberRole(memberId, role);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "워크스페이스 멤버 삭제", description = "워크스페이스에서 특정 멤버를 삭제(추방)합니다.")
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable String workspaceId,
            @PathVariable String memberId) {
        workspaceMemberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
