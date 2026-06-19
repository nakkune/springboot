import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import {
  CreateLedgerDto,
  UpdateLedgerItemDto,
  SavePayrollCodeDto,
  SaveSalaryTemplateDto,
  CreatePayrollDto,
} from './dto/payroll.dto';
import { parseSafeDate } from '../../common/utils/date';

@Injectable()
export class PayrollService {
  constructor(private readonly prisma: PrismaService) {}

  // ── 급여대장 (Payroll Ledger) ──

  async getLedgers(payYear?: number, payMonth?: number, status?: string) {
    const where: any = {};
    if (payYear) where.pay_year = payYear;
    if (payMonth) where.pay_month = payMonth;
    if (status) where.status = status;

    const ledgers = await this.prisma.erp_payroll_ledgers.findMany({
      where,
      orderBy: [{ pay_year: 'desc' }, { pay_month: 'desc' }],
    });

    return ledgers.map(l => this.mapLedger(l));
  }

  async getLedger(id: string) {
    const ledger = await this.prisma.erp_payroll_ledgers.findUnique({ where: { id } });
    if (!ledger) throw new NotFoundException(`급여대장을 찾을 수 없습니다: ${id}`);
    return this.mapLedger(ledger);
  }

  async createLedger(dto: CreateLedgerDto) {
    const ledger = await this.prisma.erp_payroll_ledgers.create({
      data: {
        title: dto.title,
        pay_year: dto.payYear,
        pay_month: dto.payMonth,
        pay_date: dto.payDate ? parseSafeDate(dto.payDate) : new Date(),
        pay_type: dto.payType || '정기급여',
        start_date: dto.startDate ? parseSafeDate(dto.startDate) : new Date(),
        end_date: dto.endDate ? parseSafeDate(dto.endDate) : new Date(),
        status: 'draft',
        created_by: dto.createdBy,
      },
    });

    // 재직 중인 모든 사원에 대해 급여 항목 생성
    const activeEmployees = await this.prisma.erp_employees.findMany({
      where: { status: 'active' },
      include: { users_erp_employees_user_idTousers: true },
    });

    for (const emp of activeEmployees) {
      await this.prisma.erp_payroll_items.create({
        data: {
          ledger_id: ledger.id,
          employee_id: emp.id,
          bank_name: emp.bank_name || '신한은행',
          bank_account: emp.bank_account || '',
          bank_owner: emp.users_erp_employees_user_idTousers?.full_name || emp.employee_no,
          gross_pay: 0,
          total_deduction: 0,
          net_pay: 0,
          status: 'draft',
        },
      });
    }

    return this.mapLedger(ledger);
  }

  async deleteLedger(id: string) {
    const existing = await this.prisma.erp_payroll_ledgers.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`삭제할 급여대장을 찾을 수 없습니다: ${id}`);
    // cascade delete items
    await this.prisma.erp_payroll_items.deleteMany({ where: { ledger_id: id } });
    await this.prisma.erp_payroll_ledgers.delete({ where: { id } });
  }

  async confirmLedger(id: string) {
    const existing = await this.prisma.erp_payroll_ledgers.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`확정할 급여대장을 찾을 수 없습니다: ${id}`);
    await this.prisma.erp_payroll_ledgers.update({ where: { id }, data: { status: 'confirmed' } });
  }

  async payLedger(id: string) {
    const existing = await this.prisma.erp_payroll_ledgers.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`지급 완료 처리할 급여대장을 찾을 수 없습니다: ${id}`);
    await this.prisma.erp_payroll_ledgers.update({ where: { id }, data: { status: 'paid' } });
  }

  // ── 급여대장 사원 항목 ──

  async getLedgerItems(ledgerId: string) {
    const items = await this.prisma.erp_payroll_items.findMany({
      where: { ledger_id: ledgerId },
      include: {
        erp_employees: {
          include: {
            users_erp_employees_user_idTousers: true,
            erp_departments_erp_employees_department_idToerp_departments: true,
          },
        },
      },
    });
    return items.map(item => this.mapItem(item));
  }

  async getLedgerItem(id: string) {
    const item = await this.prisma.erp_payroll_items.findUnique({
      where: { id },
      include: {
        erp_employees: {
          include: {
            users_erp_employees_user_idTousers: true,
            erp_departments_erp_employees_department_idToerp_departments: true,
          },
        },
      },
    });
    if (!item) throw new NotFoundException(`사원 급여 명세를 찾을 수 없습니다: ${id}`);
    return this.mapItem(item);
  }

  async updateLedgerItem(id: string, dto: UpdateLedgerItemDto) {
    const existing = await this.prisma.erp_payroll_items.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`수정할 사원 급여 명세를 찾을 수 없습니다: ${id}`);

    const updated = await this.prisma.erp_payroll_items.update({
      where: { id },
      data: {
        bank_name: dto.bankName,
        bank_account: dto.bankAccount,
        bank_owner: dto.bankOwner,
        gross_pay: dto.grossPay,
        total_deduction: dto.totalDeduction,
        net_pay: dto.netPay,
        memo: dto.memo,
      },
      include: {
        erp_employees: {
          include: {
            users_erp_employees_user_idTousers: true,
            erp_departments_erp_employees_department_idToerp_departments: true,
          },
        },
      },
    });
    return this.mapItem(updated);
  }

  async calculateLedger(ledgerId: string) {
    // 급여 계산 로직 (간략화)
    const items = await this.prisma.erp_payroll_items.findMany({
      where: { ledger_id: ledgerId },
    });
    for (const item of items) {
      const grossPay = Number(item.gross_pay || 0);
      const pension = Math.floor(grossPay * 0.045 / 10) * 10;
      const health = Math.floor(grossPay * 0.03545 / 10) * 10;
      const ltc = Math.floor(health * 0.1295 / 10) * 10;
      const employment = Math.floor(grossPay * 0.009 / 10) * 10;
      const incomeTax = Math.floor(grossPay * 0.02 / 10) * 10;
      const localTax = Math.floor(incomeTax * 0.1 / 10) * 10;
      const totalDeduction = pension + health + ltc + employment + incomeTax + localTax;
      const netPay = grossPay - totalDeduction;

      await this.prisma.erp_payroll_items.update({
        where: { id: item.id },
        data: { total_deduction: totalDeduction, net_pay: netPay },
      });
    }
  }

  // ── 급여 코드 및 템플릿 ──

  async getCodes() {
    const codes = await this.prisma.erp_payroll_codes.findMany({
      orderBy: { sort_order: 'asc' },
    });
    return codes.map(c => ({
      code: c.code,
      name: c.name,
      type: c.type,
      isTaxable: c.is_taxable,
      sortOrder: Number(c.sort_order || 0),
      isActive: c.is_active,
    }));
  }

  async saveCode(dto: SavePayrollCodeDto) {
    const existing = await this.prisma.erp_payroll_codes.findFirst({
      where: { code: dto.code },
    });
    if (existing) {
      return this.prisma.erp_payroll_codes.update({
        where: { code: existing.code },
        data: { name: dto.name, type: dto.type, sort_order: dto.sortOrder, is_active: dto.isActive },
      });
    }
    return this.prisma.erp_payroll_codes.create({
      data: { code: dto.code, name: dto.name, type: dto.type, sort_order: dto.sortOrder ?? 0, is_active: dto.isActive ?? true },
    });
  }

  async getTemplates() {
    const templates = await this.prisma.erp_salary_templates.findMany({
      include: {
        erp_employees: {
          include: { users_erp_employees_user_idTousers: true },
        },
      },
    });
    return templates.map(t => this.mapTemplate(t));
  }

  async getTemplate(employeeId: string) {
    const template = await this.prisma.erp_salary_templates.findFirst({
      where: { employee_id: employeeId },
      include: {
        erp_employees: {
          include: { users_erp_employees_user_idTousers: true },
        },
      },
    });
    if (!template) return null;
    return this.mapTemplate(template);
  }

  async saveTemplate(dto: SaveSalaryTemplateDto) {
    const existing = await this.prisma.erp_salary_templates.findFirst({
      where: { employee_id: dto.employeeId },
    });
    const data = {
      employee_id: dto.employeeId,
      base_pay: dto.basePay || 0,
      position_pay: dto.positionPay || 0,
      meal_allowance: dto.mealAllowance || 0,
      car_allowance: dto.carAllowance || 0,
      use_national_pension: dto.useNationalPension ?? true,
      use_health_insurance: dto.useHealthInsurance ?? true,
      use_employment_insurance: dto.useEmploymentInsurance ?? true,
      income_tax_rate: dto.incomeTaxRate ?? 100,
    };
    if (existing) {
      return this.prisma.erp_salary_templates.update({ where: { employee_id: existing.employee_id }, data });
    }
    return this.prisma.erp_salary_templates.create({ data });
  }

  // ── 레거시 급여 API ──

  async getPayrolls(payYear?: number, payMonth?: number, status?: string, page = 1, size = 20) {
    const where: any = {};
    if (payYear) where.pay_year = payYear;
    if (payMonth) where.pay_month = payMonth;
    if (status) where.status = status;

    const skip = (page - 1) * size;
    const [items, total] = await Promise.all([
      this.prisma.erp_payrolls.findMany({
        where, skip, take: size,
        orderBy: [{ pay_year: 'desc' }, { pay_month: 'desc' }],
        include: {
          erp_employees: {
            include: { users_erp_employees_user_idTousers: true },
          },
        },
      }),
      this.prisma.erp_payrolls.count({ where }),
    ]);

    return {
      items: items.map(p => this.mapPayroll(p)),
      total, page, size,
    };
  }

  async getEmployeePayrolls(employeeId: string) {
    const items = await this.prisma.erp_payrolls.findMany({
      where: { employee_id: employeeId },
      orderBy: [{ pay_year: 'desc' }, { pay_month: 'desc' }],
      include: {
        erp_employees: {
          include: { users_erp_employees_user_idTousers: true },
        },
      },
    });
    return items.map(p => this.mapPayroll(p));
  }

  async getPayroll(id: string) {
    const payroll = await this.prisma.erp_payrolls.findUnique({
      where: { id },
      include: {
        erp_employees: {
          include: { users_erp_employees_user_idTousers: true },
        },
      },
    });
    if (!payroll) throw new NotFoundException(`Payroll not found: ${id}`);
    return this.mapPayroll(payroll);
  }

  async createPayroll(dto: CreatePayrollDto) {
    const data = this.payrollDtoToData(dto);
    data.status = 'draft';
    const payroll = await this.prisma.erp_payrolls.create({ data });
    return this.getPayroll(payroll.id);
  }

  async bulkCreatePayroll(payYear: number, payMonth: number, payDate: string) {
    const activeEmployees = await this.prisma.erp_employees.findMany({
      where: { status: 'active' },
    });

    const results: any[] = [];
    for (const emp of activeEmployees) {
      const payroll = await this.prisma.erp_payrolls.create({
        data: {
          employee_id: emp.id,
          pay_year: payYear,
          pay_month: payMonth,
          pay_date: parseSafeDate(payDate),
          base_pay: 0,
          status: 'draft',
          gross_pay: 0,
          total_deduction: 0,
          net_pay: 0,
          created_by: emp.created_by || emp.user_id || emp.id,
        },
      });
      results.push(payroll);
    }
    return results;
  }

  async updatePayroll(id: string, dto: CreatePayrollDto) {
    const existing = await this.prisma.erp_payrolls.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Payroll not found: ${id}`);
    const data = this.payrollDtoToData(dto);
    await this.prisma.erp_payrolls.update({ where: { id }, data });
    return this.getPayroll(id);
  }

  async confirmPayroll(id: string, confirmedBy: string) {
    const existing = await this.prisma.erp_payrolls.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Payroll not found: ${id}`);
    if (existing.status !== 'draft') throw new ConflictException('Only draft payrolls can be confirmed');
    await this.prisma.erp_payrolls.update({ where: { id }, data: { status: 'confirmed', confirmed_by: confirmedBy } });
  }

  async markPaid(id: string, confirmedBy: string) {
    const existing = await this.prisma.erp_payrolls.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Payroll not found: ${id}`);
    if (existing.status !== 'confirmed') throw new ConflictException('Only confirmed payrolls can be marked as paid');
    await this.prisma.erp_payrolls.update({ where: { id }, data: { status: 'paid', confirmed_by: confirmedBy } });
  }

  async deletePayroll(id: string) {
    const existing = await this.prisma.erp_payrolls.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Payroll not found: ${id}`);
    await this.prisma.erp_payrolls.delete({ where: { id } });
  }

  // ── Mappers ──

  private mapLedger(l: any) {
    return {
      id: l.id,
      title: l.title,
      payYear: Number(l.pay_year || 0),
      payMonth: Number(l.pay_month || 0),
      payDate: l.pay_date ? l.pay_date.toISOString().split('T')[0] : null,
      payType: l.pay_type,
      startDate: l.start_date ? l.start_date.toISOString().split('T')[0] : null,
      endDate: l.end_date ? l.end_date.toISOString().split('T')[0] : null,
      status: l.status,
      createdBy: l.created_by,
      createdAt: l.created_at,
      updatedAt: l.updated_at,
      totalGross: 0,
      totalDeduction: 0,
      totalNet: 0,
      employeeCount: 0,
    };
  }

  private mapItem(item: any) {
    return {
      id: item.id,
      ledgerId: item.ledger_id,
      employeeId: item.employee_id,
      employeeName: item.erp_employees?.users_erp_employees_user_idTousers?.full_name || '',
      employeeNo: item.erp_employees?.employee_no || '',
      departmentName: item.erp_employees?.erp_departments_erp_employees_department_idToerp_departments?.name || '',
      position: item.erp_employees?.position || '',
      bankName: item.bank_name,
      bankAccount: item.bank_account,
      bankOwner: item.bank_owner,
      grossPay: Number(item.gross_pay || 0),
      totalDeduction: Number(item.total_deduction || 0),
      netPay: Number(item.net_pay || 0),
      status: item.status,
      memo: item.memo,
    };
  }

  private mapTemplate(t: any) {
    return {
      id: t.id,
      employeeId: t.employee_id,
      employeeName: t.erp_employees?.users_erp_employees_user_idTousers?.full_name || '',
      basePay: Number(t.base_pay || 0),
      positionPay: Number(t.position_pay || 0),
      mealAllowance: Number(t.meal_allowance || 0),
      carAllowance: Number(t.car_allowance || 0),
      useNationalPension: t.use_national_pension,
      useHealthInsurance: t.use_health_insurance,
      useEmploymentInsurance: t.use_employment_insurance,
      incomeTaxRate: Number(t.income_tax_rate || 100),
    };
  }

  private mapPayroll(p: any) {
    return {
      id: p.id,
      employeeId: p.employee_id,
      employeeName: p.erp_employees?.users_erp_employees_user_idTousers?.full_name || '',
      employeeNo: p.erp_employees?.employee_no || '',
      payYear: Number(p.pay_year || 0),
      payMonth: Number(p.pay_month || 0),
      payDate: p.pay_date,
      basePay: Number(p.base_pay || 0),
      positionPay: Number(p.position_pay || 0),
      overtimePay: Number(p.overtime_pay || 0),
      bonusPay: Number(p.bonus_pay || 0),
      mealAllowance: Number(p.meal_allowance || 0),
      transportation: Number(p.transportation || 0),
      grossPay: Number(p.gross_pay || 0),
      incomeTax: Number(p.income_tax || 0),
      localTax: Number(p.local_tax || 0),
      nationalPension: Number(p.national_pension || 0),
      healthInsurance: Number(p.health_insurance || 0),
      employmentInsurance: Number(p.employment_insurance || 0),
      longtermCare: Number(p.longterm_care || 0),
      totalDeduction: Number(p.total_deduction || 0),
      netPay: Number(p.net_pay || 0),
      status: p.status,
      confirmedBy: p.confirmed_by,
      createdBy: p.created_by,
    };
  }

  private payrollDtoToData(dto: CreatePayrollDto) {
    const basePay = Number(dto.basePay || 0);
    const positionPay = Number(dto.positionPay || 0);
    const overtimePay = Number(dto.overtimePay || 0);
    const bonusPay = Number(dto.bonusPay || 0);
    const mealAllowance = Number(dto.mealAllowance || 0);
    const transportation = Number(dto.transportation || 0);
    const grossPay = basePay + positionPay + overtimePay + bonusPay + mealAllowance + transportation;

    const incomeTax = Number(dto.incomeTax || 0);
    const localTax = Number(dto.localTax || 0);
    const nationalPension = Number(dto.nationalPension || 0);
    const healthInsurance = Number(dto.healthInsurance || 0);
    const employmentInsurance = Number(dto.employmentInsurance || 0);
    const longtermCare = Number(dto.longtermCare || 0);
    const totalDeduction = incomeTax + localTax + nationalPension + healthInsurance + employmentInsurance + longtermCare;

    return {
      employee_id: dto.employeeId,
      pay_year: dto.payYear,
      pay_month: dto.payMonth,
      pay_date: dto.payDate ? parseSafeDate(dto.payDate) : new Date(),
      base_pay: basePay,
      position_pay: positionPay,
      overtime_pay: overtimePay,
      bonus_pay: bonusPay,
      meal_allowance: mealAllowance,
      transportation: transportation,
      gross_pay: grossPay,
      income_tax: incomeTax,
      local_tax: localTax,
      national_pension: nationalPension,
      health_insurance: healthInsurance,
      employment_insurance: employmentInsurance,
      longterm_care: longtermCare,
      total_deduction: totalDeduction,
      net_pay: grossPay - totalDeduction,
      status: dto.status,
      created_by: dto.createdBy,
    };
  }
}
