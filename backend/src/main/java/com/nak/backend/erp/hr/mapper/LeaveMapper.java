package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.LeaveRequestDto;
import com.nak.backend.erp.hr.dto.LeaveBalanceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaveMapper {

    List<LeaveRequestDto> findByEmployeeId(@Param("employeeId") String employeeId,
                                           @Param("status") String status);

    List<LeaveRequestDto> findByManagerId(@Param("managerId") String managerId,
                                          @Param("status") String status);

    LeaveRequestDto findById(@Param("id") String id);

    int insert(LeaveRequestDto dto);

    int update(LeaveRequestDto dto);

    int updateStatus(@Param("id") String id,
                     @Param("status") String status,
                     @Param("approverId") String approverId,
                     @Param("rejectReason") String rejectReason);

    int deleteById(@Param("id") String id);

    // Leave Balance
    LeaveBalanceDto findBalance(@Param("employeeId") String employeeId,
                                @Param("year") int year);

    int upsertBalance(LeaveBalanceDto dto);
}
