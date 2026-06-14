package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.AttendanceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceMapper {

    List<AttendanceDto> findByEmployeeId(@Param("employeeId") String employeeId,
                                         @Param("fromDate") String fromDate,
                                         @Param("toDate") String toDate,
                                         @Param("status") String status);

    AttendanceDto findById(@Param("id") String id);

    AttendanceDto findByEmployeeAndDate(@Param("employeeId") String employeeId,
                                        @Param("workDate") String workDate);

    int insert(AttendanceDto dto);

    int update(AttendanceDto dto);

    int deleteById(@Param("id") String id);
}
