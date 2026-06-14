package com.nak.backend.erp.hr.service;

import com.nak.backend.erp.hr.dto.LeaveBalanceDto;
import com.nak.backend.erp.hr.dto.LeaveRequestDto;
import com.nak.backend.erp.hr.mapper.LeaveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveMapper leaveMapper;

    public List<LeaveRequestDto> getLeavesByEmployee(String employeeId, String status) {
        return leaveMapper.findByEmployeeId(employeeId, status);
    }

    public List<LeaveRequestDto> getLeavesByManager(String managerId, String status) {
        return leaveMapper.findByManagerId(managerId, status);
    }

    public LeaveRequestDto getLeave(String id) {
        LeaveRequestDto dto = leaveMapper.findById(id);
        if (dto == null) throw new NoSuchElementException("Leave request not found: " + id);
        return dto;
    }

    @Transactional
    public LeaveRequestDto createLeave(LeaveRequestDto dto) {
        dto.setId(UUID.randomUUID().toString());
        dto.setStatus("pending");
        leaveMapper.insert(dto);
        return dto;
    }

    @Transactional
    public LeaveRequestDto updateLeave(String id, LeaveRequestDto dto) {
        LeaveRequestDto existing = leaveMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Leave request not found: " + id);
        dto.setId(id);
        dto.setStatus(existing.getStatus());
        leaveMapper.update(dto);
        return dto;
    }

    @Transactional
    public void approveLeave(String id, String approverId) {
        LeaveRequestDto existing = leaveMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Leave request not found: " + id);
        if (!"pending".equals(existing.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        leaveMapper.updateStatus(id, "approved", approverId, null);

        // Update leave balance
        updateLeaveBalance(existing.getEmployeeId(),
                java.time.Year.now().getValue(),
                existing.getTotalDays(),
                false);
    }

    @Transactional
    public void rejectLeave(String id, String approverId, String rejectReason) {
        LeaveRequestDto existing = leaveMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Leave request not found: " + id);
        if (!"pending".equals(existing.getStatus())) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }
        leaveMapper.updateStatus(id, "rejected", approverId, rejectReason);
    }

    @Transactional
    public void cancelLeave(String id) {
        LeaveRequestDto existing = leaveMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Leave request not found: " + id);
        if ("approved".equals(existing.getStatus())) {
            // Restore leave balance
            updateLeaveBalance(existing.getEmployeeId(),
                    java.time.Year.now().getValue(),
                    existing.getTotalDays(),
                    true);
        }
        leaveMapper.updateStatus(id, "cancelled", null, null);
    }

    @Transactional
    public void deleteLeave(String id) {
        LeaveRequestDto existing = leaveMapper.findById(id);
        if (existing == null) throw new NoSuchElementException("Leave request not found: " + id);
        leaveMapper.deleteById(id);
    }

    // Leave Balance
    public LeaveBalanceDto getLeaveBalance(String employeeId, int year) {
        LeaveBalanceDto balance = leaveMapper.findBalance(employeeId, year);
        if (balance == null) {
            balance = new LeaveBalanceDto();
            balance.setEmployeeId(employeeId);
            balance.setYear(year);
            balance.setTotalDays(BigDecimal.valueOf(15));
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(BigDecimal.valueOf(15));
        }
        return balance;
    }

    @Transactional
    public void updateLeaveBalance(String employeeId, int year, BigDecimal days, boolean isRestore) {
        LeaveBalanceDto balance = leaveMapper.findBalance(employeeId, year);
        if (balance == null) {
            balance = new LeaveBalanceDto();
            balance.setId(UUID.randomUUID().toString());
            balance.setEmployeeId(employeeId);
            balance.setYear(year);
            balance.setTotalDays(BigDecimal.valueOf(15));
            balance.setUsedDays(isRestore ? BigDecimal.ZERO : days);
            balance.setRemainingDays(BigDecimal.valueOf(15).subtract(isRestore ? BigDecimal.ZERO : days));
        } else {
            if (isRestore) {
                balance.setUsedDays(balance.getUsedDays().subtract(days));
            } else {
                balance.setUsedDays(balance.getUsedDays().add(days));
            }
            balance.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()));
        }
        leaveMapper.upsertBalance(balance);
    }
}
