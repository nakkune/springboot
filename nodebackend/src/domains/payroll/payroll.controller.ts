import { Controller, Get, Post, Put, Delete, Param, Body, Query, HttpCode, HttpStatus } from '@nestjs/common';
import { PayrollService } from './payroll.service';
import {
  CreateLedgerDto,
  UpdateLedgerItemDto,
  SavePayrollCodeDto,
  SaveSalaryTemplateDto,
  CreatePayrollDto,
  BulkCreatePayrollDto,
  ConfirmPayrollDto,
} from './dto/payroll.dto';

@Controller('erp/hr/payrolls')
export class PayrollController {
  constructor(private readonly payrollService: PayrollService) {}

  // ── 급여대장 (Payroll Ledger) ──
  @Get('ledgers')
  getLedgers(
    @Query('payYear') payYear?: string,
    @Query('payMonth') payMonth?: string,
    @Query('status') status?: string
  ) {
    return this.payrollService.getLedgers(
      payYear ? Number(payYear) : undefined,
      payMonth ? Number(payMonth) : undefined,
      status,
    );
  }

  @Get('ledgers/:id')
  getLedger(@Param('id') id: string) {
    return this.payrollService.getLedger(id);
  }

  @Post('ledgers')
  @HttpCode(HttpStatus.CREATED)
  createLedger(@Body() dto: CreateLedgerDto) {
    return this.payrollService.createLedger(dto);
  }

  @Delete('ledgers/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteLedger(@Param('id') id: string) {
    return this.payrollService.deleteLedger(id);
  }

  @Post('ledgers/:id/confirm')
  confirmLedger(@Param('id') id: string) {
    return this.payrollService.confirmLedger(id);
  }

  @Post('ledgers/:id/pay')
  payLedger(@Param('id') id: string) {
    return this.payrollService.payLedger(id);
  }

  // ── 급여대장 사원 항목 (Payroll Items) ──
  @Get('ledgers/:ledgerId/items')
  getLedgerItems(@Param('ledgerId') ledgerId: string) {
    return this.payrollService.getLedgerItems(ledgerId);
  }

  @Get('ledgers/items/:id')
  getLedgerItem(@Param('id') id: string) {
    return this.payrollService.getLedgerItem(id);
  }

  @Put('ledgers/items/:id')
  updateLedgerItem(@Param('id') id: string, @Body() dto: UpdateLedgerItemDto) {
    return this.payrollService.updateLedgerItem(id, dto);
  }

  @Post('ledgers/:id/calculate')
  calculateLedger(@Param('id') id: string) {
    return this.payrollService.calculateLedger(id);
  }

  // ── 급여 코드 및 템플릿 ──
  @Get('codes')
  getCodes() {
    return this.payrollService.getCodes();
  }

  @Post('codes')
  saveCode(@Body() dto: SavePayrollCodeDto) {
    return this.payrollService.saveCode(dto);
  }

  @Get('templates')
  getTemplates() {
    return this.payrollService.getTemplates();
  }

  @Get('templates/:employeeId')
  getTemplate(@Param('employeeId') employeeId: string) {
    return this.payrollService.getTemplate(employeeId);
  }

  @Post('templates')
  saveTemplate(@Body() dto: SaveSalaryTemplateDto) {
    return this.payrollService.saveTemplate(dto);
  }

  // ── 레거시 급여 API ──
  @Get()
  getPayrolls(
    @Query('payYear') payYear?: string,
    @Query('payMonth') payMonth?: string,
    @Query('status') status?: string,
    @Query('page') page: string = '1',
    @Query('size') size: string = '20'
  ) {
    return this.payrollService.getPayrolls(
      payYear ? Number(payYear) : undefined,
      payMonth ? Number(payMonth) : undefined,
      status,
      Number(page),
      Number(size),
    );
  }

  @Get('employee')
  getEmployeePayrolls(@Query('employeeId') employeeId: string) {
    return this.payrollService.getEmployeePayrolls(employeeId);
  }

  @Get(':id')
  getPayroll(@Param('id') id: string) {
    return this.payrollService.getPayroll(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  createPayroll(@Body() dto: CreatePayrollDto) {
    return this.payrollService.createPayroll(dto);
  }

  @Post('bulk-create')
  @HttpCode(HttpStatus.CREATED)
  bulkCreatePayroll(@Body() body: BulkCreatePayrollDto) {
    return this.payrollService.bulkCreatePayroll(body.payYear, body.payMonth, body.payDate);
  }

  @Put(':id')
  updatePayroll(@Param('id') id: string, @Body() dto: CreatePayrollDto) {
    return this.payrollService.updatePayroll(id, dto);
  }

  @Post(':id/confirm')
  confirmPayroll(@Param('id') id: string, @Body() body: ConfirmPayrollDto) {
    return this.payrollService.confirmPayroll(id, body.confirmedBy);
  }

  @Post(':id/pay')
  markPaid(@Param('id') id: string, @Body() body: ConfirmPayrollDto) {
    return this.payrollService.markPaid(id, body.confirmedBy);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deletePayroll(@Param('id') id: string) {
    return this.payrollService.deletePayroll(id);
  }
}
