package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.DepartmentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP 부서 Mapper
 */
@Mapper
public interface DepartmentMapper {

    List<DepartmentDto> findAll();

    DepartmentDto findById(@Param("id") String id);

    DepartmentDto findByName(@Param("name") String name);

    List<DepartmentDto> findByParentId(@Param("parentId") String parentId);

    int insert(DepartmentDto department);

    int update(DepartmentDto department);

    int deleteById(@Param("id") String id);
}
