package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.EmployeeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP 직원 Mapper
 */
@Mapper
public interface EmployeeMapper {

    List<EmployeeDto> findAll(
            @Param("departmentId") String departmentId,
            @Param("status") String status,
            @Param("search") String search,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countAll(
            @Param("departmentId") String departmentId,
            @Param("status") String status,
            @Param("search") String search
    );

    EmployeeDto findById(@Param("id") String id);

    EmployeeDto findByUserId(@Param("userId") String userId);

    EmployeeDto findByEmployeeNo(@Param("employeeNo") String employeeNo);

    String findLastEmployeeNoByPattern(@Param("pattern") String pattern);

    int insert(EmployeeDto employee);

    int update(EmployeeDto employee);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    int deleteById(@Param("id") String id);
}
