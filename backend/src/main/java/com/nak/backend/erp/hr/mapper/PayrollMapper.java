package com.nak.backend.erp.hr.mapper;

import com.nak.backend.erp.hr.dto.PayrollDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayrollMapper {

    // ── 1. 급여대장 (Payroll Ledger) ──
    List<PayrollDto.PayrollLedgerDto> findAllLedgers(
            @Param("payYear") Integer payYear,
            @Param("payMonth") Integer payMonth,
            @Param("status") String status
    );

    PayrollDto.PayrollLedgerDto findLedgerById(@Param("id") String id);

    int insertLedger(PayrollDto.PayrollLedgerDto dto);

    int updateLedger(PayrollDto.PayrollLedgerDto dto);

    int updateLedgerStatus(@Param("id") String id, @Param("status") String status);

    int deleteLedgerById(@Param("id") String id);


    // ── 2. 급여코드 (Payroll Code) ──
    List<PayrollDto.PayrollCodeDto> findAllCodes();

    PayrollDto.PayrollCodeDto findCodeByCode(@Param("code") String code);

    int insertCode(PayrollDto.PayrollCodeDto dto);

    int updateCode(PayrollDto.PayrollCodeDto dto);

    int deleteCodeByCode(@Param("code") String code);


    // ── 3. 급여계약 템플릿 (Salary Template) ──
    List<PayrollDto.SalaryTemplateDto> findAllTemplates();

    PayrollDto.SalaryTemplateDto findTemplateByEmployeeId(@Param("employeeId") String employeeId);

    int insertTemplate(PayrollDto.SalaryTemplateDto dto);

    int updateTemplate(PayrollDto.SalaryTemplateDto dto);

    int deleteTemplateByEmployeeId(@Param("employeeId") String employeeId);


    // ── 4. 대장 사원별 마스터 (Payroll Item) ──
    List<PayrollDto.PayrollItemDto> findItemsByLedgerId(@Param("ledgerId") String ledgerId);

    PayrollDto.PayrollItemDto findItemById(@Param("id") String id);

    PayrollDto.PayrollItemDto findItemByLedgerAndEmployee(
            @Param("ledgerId") String ledgerId,
            @Param("employeeId") String employeeId
    );

    int insertItem(PayrollDto.PayrollItemDto dto);

    int updateItem(PayrollDto.PayrollItemDto dto);

    int deleteItemById(@Param("id") String id);

    int deleteItemsByLedgerId(@Param("ledgerId") String ledgerId);


    // ── 5. 사원별 급여 세부 내역 (Payroll Detail) ──
    List<PayrollDto.PayrollDetailDto> findDetailsByItemId(@Param("payrollItemId") String payrollItemId);

    int insertDetail(PayrollDto.PayrollDetailDto dto);

    int deleteDetailsByItemId(@Param("payrollItemId") String payrollItemId);

    int insertDetailsBulk(@Param("details") List<PayrollDto.PayrollDetailDto> details);


    // ── [호환성 보존] 기존 erp_payrolls 기반 메소드 ──
    List<PayrollDto> findAll(@Param("payYear") Integer payYear,
                             @Param("payMonth") Integer payMonth,
                             @Param("status") String status,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    int countAll(@Param("payYear") Integer payYear,
                 @Param("payMonth") Integer payMonth,
                 @Param("status") String status);

    List<PayrollDto> findByEmployeeId(@Param("employeeId") String employeeId);

    PayrollDto findById(@Param("id") String id);

    int insert(PayrollDto dto);

    int update(PayrollDto dto);

    int updateStatus(@Param("id") String id,
                     @Param("status") String status,
                     @Param("confirmedBy") String confirmedBy);

    int deleteById(@Param("id") String id);
}
