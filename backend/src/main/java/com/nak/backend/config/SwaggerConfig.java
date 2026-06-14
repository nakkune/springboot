package com.nak.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 설정 클래스
 * springdoc-openapi 라이브러리를 사용하여 API 문서를 정의하고 JWT 인증을 지원하도록 설정합니다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "jwtAuth";
        
        // API 요청 시 JWT 토큰을 Header에 담아 보낼 수 있도록 SecurityRequirement 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        
        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("MiniERP API Documentation")
                        .description("MiniERP 시스템의 백엔드 REST API 문서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
