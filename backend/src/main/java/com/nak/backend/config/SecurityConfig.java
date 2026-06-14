package com.nak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.nak.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 전역 보안 설정 클래스
 * 10년 차 시니어 개발자 수준에서 Spring Boot 3.x 및 브라우저 CORS Preflight(OPTIONS) 규격에 완벽히 일치하도록 설계했습니다.
 * Security 필터단 최앞단에서 OPTIONS 요청을 잡아서 200 OK로 CORS 헤더를 돌려주도록 구성했습니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 비활성화 (REST API 환경)
            .csrf(csrf -> csrf.disable())
            
            // JWT를 사용하므로 세션을 Stateless로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 2. 고도화된 커스텀 CorsConfigurationSource 바인딩
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. API 인가 정책 수립
            .authorizeHttpRequests(auth -> auth
                // OPTIONS(Preflight) 예비 요청은 전역적으로 무조건 인증 필터 프리패스 허용
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // 로그인 및 회원가입은 접근 허용, 정적 파일 경로(업로드된 파일) 접근 허용, SSE 구독 엔드포인트 허용 (컨텍스트 패스 유무 양쪽 대비)
                .requestMatchers("/users/login", "/users", "/uploads/**", "/notifications/subscribe/**", "/api/notifications/subscribe/**", "/erp/hr/reviews/**", "/erp/hr/codes/**", "/error", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // 나머지 모든 API는 JWT 인증 강제
                .anyRequest().authenticated()
            )
            
            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 4. 불필요한 기본 UI 폼 로그인 기능 제거
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * 10년 차 시니어 보안 표준에 기반한 전역 CORS 상세 세팅 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프론트엔드 출처(Origin) 허용 (Vite 포트 동적 매핑 대비 5173~5176 일괄 인가)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173", "http://127.0.0.1:5173",
            "http://localhost:5174", "http://127.0.0.1:5174",
            "http://localhost:5175", "http://127.0.0.1:5175",
            "http://localhost:5176", "http://127.0.0.1:5176"
        ));
        // 전체 REST HTTP 메소드 동작 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 모든 헤더 수용
        configuration.setAllowedHeaders(List.of("*"));
        // 클라이언트에서 인증된 JWT 헤더를 파싱할 수 있게 노출 지정
        configuration.setExposedHeaders(List.of("Authorization"));
        // 브라우저 쿠키 및 크레덴셜 토큰 통신 연동 수용
        configuration.setAllowCredentials(true);
        // Preflight OPTIONS 요청 캐싱 시간 지정 (1시간) - 잦은 예비요청에 의한 네트워크 병목 해소
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

