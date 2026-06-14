package com.nak.backend.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nak.backend.user.dto.UserDto;

/**
 * MyBatis User Mapper 인터페이스
 */
@Mapper
public interface UserMapper {
    List<UserDto> findAll();
    UserDto findById(@Param("id") String id);
    UserDto findByEmail(@Param("email") String email); // [NEW] 이메일 단건 조회 추가
    List<UserDto> findByFullName(@Param("fullName") String fullName);
    int insert(UserDto user);
    int update(UserDto user);
    int deleteById(@Param("id") String id);
    int count();
}
