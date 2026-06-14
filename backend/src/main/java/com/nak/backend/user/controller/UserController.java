package com.nak.backend.user.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nak.backend.user.dto.UserDto;
import com.nak.backend.user.service.UserService;
import com.nak.backend.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 REST API 컨트롤러
 */
@Tag(name = "사용자 (User)", description = "ERP 사용자 관리 - 가입, 회원 조회, 로그인 API를 제공합니다.")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "전체 회원 목록 조회", description = "시스템에 등록된 전체 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "회원 단건 조회", description = "사용자 ID를 기준으로 특정 회원의 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "회원 이름 검색", description = "사용자의 전체 이름을 기준으로 검색어와 매칭되는 회원 목록을 조회합니다.")
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchByFullName(@RequestParam String fullName) {
        return ResponseEntity.ok(userService.searchByFullName(fullName));
    }

    @Operation(summary = "전체 회원 수 조회", description = "시스템에 등록된 전체 가입자 수를 반환합니다.")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCount() {
        return ResponseEntity.ok(Map.of("count", userService.getUserCount()));
    }

    @Operation(summary = "신규 회원 가입 및 등록", description = "새로운 사용자 계정을 시스템에 등록합니다.")
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(userService.createUser(user));
    }

    @Operation(summary = "회원 정보 수정", description = "사용자 ID를 기준으로 이름, 이메일, 활성화 여부 등 회원 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String id,
            @RequestBody UserDto user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @Operation(summary = "회원 탈퇴 및 계정 삭제", description = "사용자 ID를 기준으로 회원 정보를 완전히 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이메일과 비밀번호를 통한 인증 로그인 API
     */
    @Operation(summary = "로그인 인증 (JWT 발급)", description = "이메일과 비밀번호를 전송하여 로그인 세션을 인증받고 JWT 토큰 및 회원 정보를 획득합니다.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        UserDto user = userService.login(email, password);

        if (user != null) {
            // 로그인 성공 시 JwtTokenProvider를 통해 실제 JWT 발급
            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "user", user
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("success", false, "error", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
