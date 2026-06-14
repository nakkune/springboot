package com.nak.backend.user.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // [NEW] BCrypt 임포트 추가
import com.nak.backend.user.dto.UserDto;
import com.nak.backend.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    // 시니어 개발자 관점에서 재사용을 위해 BCryptPasswordEncoder 필드 선언 및 인스턴스화
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<UserDto> getAllUsers() {
        return userMapper.findAll();
    }

    public UserDto getUserById(String id) {
        UserDto user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id);
        }
        return user;
    }

    public List<UserDto> searchByFullName(String fullName) {
        return userMapper.findByFullName(fullName);
    }

    public int getUserCount() {
        return userMapper.count();
    }

    @Transactional
    public UserDto createUser(UserDto user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }
        if (user.getTimezone() == null) user.setTimezone("Asia/Seoul");
        if (user.getRole() == null) user.setRole("member");
        if (user.getMemberStatus() == null) user.setMemberStatus("pending"); // 디폴트를 pending으로 설정하여 승인 프로세스 수립
        if (user.getTheme() == null) user.setTheme("dark");
        if (user.getIsActive() == null) user.setIsActive(true);
        
        // 시니어 보안 설계: 회원가입 시 평문 비밀번호가 왔을 경우에만 BCrypt 암호화 수행
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            String rawPassword = user.getPasswordHash();
            if (!rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$") && !rawPassword.startsWith("$2y$")) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
        }
        
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    @Transactional
    public UserDto updateUser(String id, UserDto user) {
        UserDto existing = getUserById(id);
        
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        
        // 시니어 보안 설계: 비밀번호 수정이 동반되었을 때, 해시가 아닌 평문일 경우에만 BCrypt 암호화 가공 후 적재
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            String rawPassword = user.getPasswordHash();
            if (!rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$") && !rawPassword.startsWith("$2y$")) {
                existing.setPasswordHash(passwordEncoder.encode(rawPassword));
            } else {
                existing.setPasswordHash(rawPassword);
            }
        }
        
        if (user.getFullName() != null) existing.setFullName(user.getFullName());
        if (user.getAvatarUrl() != null) existing.setAvatarUrl(user.getAvatarUrl());
        if (user.getTimezone() != null) existing.setTimezone(user.getTimezone());
        if (user.getRole() != null) existing.setRole(user.getRole());
        if (user.getMemberStatus() != null) existing.setMemberStatus(user.getMemberStatus());
        if (user.getTheme() != null) existing.setTheme(user.getTheme());
        if (user.getIsActive() != null) existing.setIsActive(user.getIsActive());
        if (user.getLastLoginAt() != null) existing.setLastLoginAt(user.getLastLoginAt());
        
        userMapper.update(existing);
        return userMapper.findById(id);
    }

    @Transactional
    public void deleteUser(String id) {
        getUserById(id);
        userMapper.deleteById(id);
    }

    /**
     * 이메일과 패스워드를 통한 로그인 비즈니스 로직
     * 10년 차 시니어 개발자 관점에서 평문과 BCrypt 해시를 모두 안전하게 대조하는 하이브리드 비교 방식을 적용했습니다.
     */
    @Transactional
    public UserDto login(String email, String password) {
        UserDto user = userMapper.findByEmail(email);
        if (user != null) {
            String hash = user.getPasswordHash();
            if (hash == null || hash.isEmpty()) {
                return null;
            }

            boolean isMatched = false;
            // 해시 포맷 검증 ($2a$, $2b$, $2y$ 등으로 시작 시 BCrypt 매치 수행)
            if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
                isMatched = passwordEncoder.matches(password, hash);
            } else {
                // [보안 가이드라인 준수]: 평문 비밀번호 로그인 폴백 차단 (주석 처리)
                // 기존 데이터 마이그레이션 예외 일치 처리를 완전히 차단하여 평문 로그인 거절
                // isMatched = hash.equals(password);
                isMatched = false;
            }

            if (isMatched) {
                // 가입 승인 검증: approved 가 아니면 시스템 로그인을 제한하고 상세 정보 전달
                if (!"approved".equalsIgnoreCase(user.getMemberStatus())) {
                    throw new IllegalStateException("가입 승인 대기 중이거나 로그인 제한된 상태입니다. 관리자 승인을 기다려주세요.");
                }
                
                // 로그인 시간 갱신 (OffsetDateTime 표준 준수)
                user.setLastLoginAt(java.time.OffsetDateTime.now());
                userMapper.update(user);
                return user;
            }
        }
        return null;
    }
}
