package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.AttendanceDto;
import com.nak.backend.erp.hr.mapper.AttendanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceMapper attendanceMapper;

    public List<AttendanceDto> getAttendance(String employeeId, String fromDate, String toDate, String status) {
        return attendanceMapper.findByEmployeeId(employeeId, fromDate, toDate, status);
    }

    public AttendanceDto getAttendanceById(String id) {
        AttendanceDto dto = attendanceMapper.findById(id);
        if (dto == null) throw new NoSuchElementException("Attendance not found: " + id);
        return dto;
    }

    @Transactional
    public AttendanceDto createAttendance(AttendanceDto dto) {
        dto.setId(UUID.randomUUID().toString());
        if (dto.getWorkHours() == null) {
            dto.setWorkHours(BigDecimal.ZERO);
        }
        if (dto.getOvertimeHours() == null) {
            dto.setOvertimeHours(BigDecimal.ZERO);
        }
        if (dto.getStatus() == null) {
            dto.setStatus("present");
        }

        if (dto.getCheckIn() != null && dto.getCheckOut() != null) {
            long minutes = Duration.between(dto.getCheckIn(), dto.getCheckOut()).toMinutes();
            BigDecimal workHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP);
            dto.setWorkHours(workHours);
            if (workHours.compareTo(BigDecimal.valueOf(8)) > 0) {
                dto.setOvertimeHours(workHours.subtract(BigDecimal.valueOf(8)));
            } else {
                dto.setOvertimeHours(BigDecimal.ZERO);
            }
        }

        attendanceMapper.insert(dto);
        return dto;
    }

    @Transactional
    public AttendanceDto checkIn(String employeeId) {
        String today = java.time.LocalDate.now().toString();
        AttendanceDto existing = attendanceMapper.findByEmployeeAndDate(employeeId, today);
        if (existing != null) {
            throw new IllegalStateException("Already checked in today");
        }

        AttendanceDto dto = new AttendanceDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmployeeId(employeeId);
        dto.setWorkDate(today);
        dto.setCheckIn(OffsetDateTime.now());
        dto.setStatus("present");
        dto.setWorkHours(BigDecimal.ZERO);
        dto.setOvertimeHours(BigDecimal.ZERO);

        attendanceMapper.insert(dto);
        return dto;
    }

    @Transactional
    public AttendanceDto checkOut(String employeeId) {
        String today = java.time.LocalDate.now().toString();
        AttendanceDto dto = attendanceMapper.findByEmployeeAndDate(employeeId, today);
        if (dto == null) {
            throw new IllegalStateException("No check-in record found for today");
        }
        if (dto.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out today");
        }

        OffsetDateTime now = OffsetDateTime.now();
        dto.setCheckOut(now);

        if (dto.getCheckIn() != null) {
            long hours = Duration.between(dto.getCheckIn(), now).toMinutes();
            BigDecimal workHours = BigDecimal.valueOf(hours).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP);
            dto.setWorkHours(workHours);

            // 8시간 초과시 연장근무
            if (workHours.compareTo(BigDecimal.valueOf(8)) > 0) {
                dto.setOvertimeHours(workHours.subtract(BigDecimal.valueOf(8)));
            }
        }

        attendanceMapper.update(dto);
        return dto;
    }

    @Transactional
    public AttendanceDto updateAttendance(String id, AttendanceDto dto) {
        AttendanceDto existing = attendanceMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Attendance not found: " + id);
        dto.setId(id);
        attendanceMapper.update(dto);
        return dto;
    }

    @Transactional
    public void deleteAttendance(String id) {
        AttendanceDto existing = attendanceMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Attendance not found: " + id);
        attendanceMapper.deleteById(id);
    }
}
