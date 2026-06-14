package com.nak.backend.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan("com.nak.backend.user.mapper")  // 매퍼 인터페이스 패키지 스캔
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // XML 매퍼 파일 경로
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml")
        );

        // MyBatis 기본 설정
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);   // snake_case → camelCase
        config.setCallSettersOnNulls(true);          // null 값도 setter 호출
        config.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);   // null 타입 처리

        factoryBean.setConfiguration(config);

        return factoryBean.getObject();
    }    
}
