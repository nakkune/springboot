package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.PerformanceReviewDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * ERP 인사 평가 Mapper
 */
@Mapper
public interface PerformanceMapper {

    List<PerformanceReviewDto> findAll(
            @Param("employeeId") String employeeId,
            @Param("reviewYear") Integer reviewYear,
            @Param("reviewPeriod") String reviewPeriod,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countAll(
            @Param("employeeId") String employeeId,
            @Param("reviewYear") Integer reviewYear,
            @Param("reviewPeriod") String reviewPeriod,
            @Param("status") String status
    );

    List<PerformanceReviewDto> findByEmployeeId(
            @Param("employeeId") String employeeId
    );

    PerformanceReviewDto findById(@Param("id") String id);

    int insert(PerformanceReviewDto dto);

    int update(PerformanceReviewDto dto);

    int updateStatus(
            @Param("id") String id,
            @Param("status") String status,
            @Param("submittedAt") OffsetDateTime submittedAt
    );

    int deleteById(@Param("id") String id);
}
