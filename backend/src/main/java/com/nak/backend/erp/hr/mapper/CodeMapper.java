package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.CodeDto;
import com.nak.backend.erp.hr.dto.CodeGroupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP 공통 코드 Mapper
 */
@Mapper
public interface CodeMapper {

    List<CodeDto> findByGroup(@Param("codeGroup") String codeGroup);

    List<CodeDto> findAll();

    List<CodeGroupDto> findGroups();

    CodeDto findById(@Param("id") String id);

    CodeDto findByGroupAndCode(@Param("codeGroup") String codeGroup,
                               @Param("code") String code);

    int insert(CodeDto dto);

    int update(CodeDto dto);

    int deleteById(@Param("id") String id);
}
