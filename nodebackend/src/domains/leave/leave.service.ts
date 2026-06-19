import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateLeaveDto, UpdateLeaveDto } from './dto/leave.dto';
import { parseSafeDate } from '../../common/utils/date';

@Injectable()
export class LeaveService {
  constructor(private readonly prisma: PrismaService) {}

  private mapLeave(record: any) {
    return {
      id: record.id,
      employeeId: record.employee_id,
      employeeName: record.erp_employees_erp_leave_requests_employee_idToerp_employees?.users_erp_employees_user_idTousers?.full_name || '',
      employeeNo: record.erp_employees_erp_leave_requests_employee_idToerp_employees?.employee_no || '',
      leaveType: record.leave_type,
      startDate: record.start_date ? record.start_date.toISOString().split('T')[0] : null,
      endDate: record.end_date ? record.end_date.toISOString().split('T')[0] : null,
      totalDays: Number(record.total_days || 0),
      reason: record.reason,
      status: record.status,
      approverId: record.approver_id,
      approverName: record.erp_employees_erp_leave_requests_approver_idToerp_employees?.users_erp_employees_user_idTousers?.full_name || null,
      rejectReason: record.reject_reason,
      createdAt: record.created_at,
    };
  }

  private includeRelations() {
    return {
      erp_employees_erp_leave_requests_employee_idToerp_employees: {
        include: { users_erp_employees_user_idTousers: true },
      },
      erp_employees_erp_leave_requests_approver_idToerp_employees: {
        include: { users_erp_employees_user_idTousers: true },
      },
    };
  }

  async getLeaves(employeeId?: string, managerId?: string, status?: string) {
    const where: any = {};
    if (employeeId) where.employee_id = employeeId;
    if (managerId) {
      where.OR = [
        { approver_id: managerId },
        {
          erp_employees_erp_leave_requests_employee_idToerp_employees: {
            erp_departments_erp_employees_department_idToerp_departments: {
              manager_id: managerId,
            },
          },
        },
      ];
    }
    if (status) where.status = status;

    const records = await this.prisma.erp_leave_requests.findMany({
      where,
      orderBy: { start_date: 'desc' },
      include: this.includeRelations(),
    });

    return records.map(record => this.mapLeave(record));
  }

  async getLeave(id: string) {
    const record = await this.prisma.erp_leave_requests.findUnique({
      where: { id },
      include: this.includeRelations(),
    });
    if (!record) throw new NotFoundException(`Leave request not found: ${id}`);
    return this.mapLeave(record);
  }

  async createLeave(dto: CreateLeaveDto) {
    const record = await this.prisma.erp_leave_requests.create({
      data: {
        employee_id: dto.employeeId,
        leave_type: dto.leaveType,
        start_date: parseSafeDate(dto.startDate),
        end_date: parseSafeDate(dto.endDate),
        total_days: dto.totalDays,
        reason: dto.reason || '',
        status: 'pending',
        created_by: dto.createdBy || dto.employeeId,
      },
      include: this.includeRelations(),
    });
    return this.mapLeave(record);
  }

  async updateLeave(id: string, dto: UpdateLeaveDto) {
    const existing = await this.prisma.erp_leave_requests.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Leave request not found: ${id}`);

    const record = await this.prisma.erp_leave_requests.update({
      where: { id },
      data: {
        leave_type: dto.leaveType,
        start_date: dto.startDate ? parseSafeDate(dto.startDate) : undefined,
        end_date: dto.endDate ? parseSafeDate(dto.endDate) : undefined,
        total_days: dto.totalDays,
        reason: dto.reason,
      },
      include: this.includeRelations(),
    });
    return this.mapLeave(record);
  }

  async approveLeave(id: string, approverId: string) {
    const existing = await this.prisma.erp_leave_requests.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Leave request not found: ${id}`);
    if (existing.status !== 'pending') {
      throw new ConflictException('Only pending requests can be approved');
    }

    await this.prisma.erp_leave_requests.update({
      where: { id },
      data: { status: 'approved', approver_id: approverId },
    });

    // 잔여 휴가 차감
    await this.updateLeaveBalance(existing.employee_id, new Date().getFullYear(), Number(existing.total_days), false);
  }

  async rejectLeave(id: string, approverId: string, rejectReason?: string) {
    const existing = await this.prisma.erp_leave_requests.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Leave request not found: ${id}`);
    if (existing.status !== 'pending') {
      throw new ConflictException('Only pending requests can be rejected');
    }

    await this.prisma.erp_leave_requests.update({
      where: { id },
      data: { status: 'rejected', approver_id: approverId, reject_reason: rejectReason || null },
    });
  }

  async cancelLeave(id: string) {
    const existing = await this.prisma.erp_leave_requests.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Leave request not found: ${id}`);

    if (existing.status === 'approved') {
      // 잔여 휴가 복구
      await this.updateLeaveBalance(existing.employee_id, new Date().getFullYear(), Number(existing.total_days), true);
    }

    await this.prisma.erp_leave_requests.update({
      where: { id },
      data: { status: 'cancelled' },
    });
  }

  async deleteLeave(id: string) {
    const existing = await this.prisma.erp_leave_requests.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Leave request not found: ${id}`);
    await this.prisma.erp_leave_requests.delete({ where: { id } });
  }

  async getLeaveBalance(employeeId: string, year: number) {
    const balance = await this.prisma.erp_leave_balances.findFirst({
      where: { employee_id: employeeId, year },
    });

    if (!balance) {
      return {
        id: '',
        employeeId,
        employeeName: '',
        employeeNo: '',
        year,
        totalDays: 15,
        usedDays: 0,
        remainingDays: 15,
      };
    }
    return {
      id: balance.id,
      employeeId: balance.employee_id,
      employeeName: '',
      employeeNo: '',
      year: balance.year,
      totalDays: Number(balance.total_days || 0),
      usedDays: Number(balance.used_days || 0),
      remainingDays: Number(balance.remaining_days || 0),
    };
  }

  private async updateLeaveBalance(employeeId: string, year: number, days: number, isRestore: boolean) {
    const balance = await this.prisma.erp_leave_balances.findFirst({
      where: { employee_id: employeeId, year },
    });

    if (!balance) {
      const usedDays = isRestore ? 0 : days;
      await this.prisma.erp_leave_balances.create({
        data: {
          employee_id: employeeId,
          year,
          total_days: 15,
          used_days: usedDays,
          remaining_days: 15 - usedDays,
        },
      });
    } else {
      const newUsed = isRestore
        ? Number(balance.used_days) - days
        : Number(balance.used_days) + days;
      const newRemaining = Number(balance.total_days) - newUsed;

      await this.prisma.erp_leave_balances.update({
        where: { id: balance.id },
        data: {
          used_days: newUsed,
          remaining_days: newRemaining,
        },
      });
    }
  }
}
