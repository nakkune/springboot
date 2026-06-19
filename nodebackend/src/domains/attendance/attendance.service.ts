import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateAttendanceDto, UpdateAttendanceDto } from './dto/attendance.dto';
import { parseSafeDate } from '../../common/utils/date';

@Injectable()
export class AttendanceService {
  constructor(private readonly prisma: PrismaService) {}

  private mapAttendance(record: any) {
    return {
      id: record.id,
      employeeId: record.employee_id,
      employeeName: record.erp_employees?.users_erp_employees_user_idTousers?.full_name || '',
      employeeNo: record.erp_employees?.employee_no || '',
      workDate: record.work_date ? record.work_date.toISOString().split('T')[0] : null,
      checkIn: record.check_in,
      checkOut: record.check_out,
      workHours: Number(record.work_hours || 0),
      overtimeHours: Number(record.overtime_hours || 0),
      status: record.status,
      memo: record.memo,
    };
  }

  private includeRelations() {
    return {
      erp_employees: {
        include: {
          users_erp_employees_user_idTousers: true,
        },
      },
    };
  }

  async getAttendance(employeeId?: string, fromDate?: string, toDate?: string, status?: string) {
    const where: any = {};
    if (employeeId) where.employee_id = employeeId;
    if (status) where.status = status;
    if (fromDate || toDate) {
      where.work_date = {};
      if (fromDate) where.work_date.gte = parseSafeDate(fromDate);
      if (toDate) where.work_date.lte = parseSafeDate(toDate);
    }

    const records = await this.prisma.erp_attendance.findMany({
      where,
      orderBy: { work_date: 'desc' },
      include: this.includeRelations(),
    });

    return records.map(record => this.mapAttendance(record));
  }

  async getAttendanceById(id: string) {
    const record = await this.prisma.erp_attendance.findUnique({
      where: { id },
      include: this.includeRelations(),
    });
    if (!record) throw new NotFoundException(`Attendance not found: ${id}`);
    return this.mapAttendance(record);
  }

  async createAttendance(dto: CreateAttendanceDto) {
    const data: any = {
      employee_id: dto.employeeId,
      work_date: dto.workDate ? parseSafeDate(dto.workDate) : new Date(),
      check_in: dto.checkIn ? parseSafeDate(dto.checkIn) : null,
      check_out: dto.checkOut ? parseSafeDate(dto.checkOut) : null,
      work_hours: dto.workHours ?? 0,
      overtime_hours: dto.overtimeHours ?? 0,
      status: dto.status || 'present',
      memo: dto.memo || null,
    };

    // checkIn과 checkOut이 모두 있으면 근무시간 자동 계산
    if (data.check_in && data.check_out) {
      const diffMs = data.check_out.getTime() - data.check_in.getTime();
      const hours = Math.round((diffMs / (1000 * 60 * 60)) * 10) / 10;
      data.work_hours = hours;
      data.overtime_hours = hours > 8 ? Math.round((hours - 8) * 10) / 10 : 0;
    }

    const record = await this.prisma.erp_attendance.create({
      data,
      include: this.includeRelations(),
    });
    return this.mapAttendance(record);
  }

  async checkIn(employeeId: string) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const existing = await this.prisma.erp_attendance.findFirst({
      where: {
        employee_id: employeeId,
        work_date: { gte: today, lt: tomorrow },
      },
    });

    if (existing) {
      throw new ConflictException('Already checked in today');
    }

    const record = await this.prisma.erp_attendance.create({
      data: {
        employee_id: employeeId,
        work_date: today,
        check_in: new Date(),
        status: 'present',
        work_hours: 0,
        overtime_hours: 0,
      },
      include: this.includeRelations(),
    });
    return this.mapAttendance(record);
  }

  async checkOut(employeeId: string) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const record = await this.prisma.erp_attendance.findFirst({
      where: {
        employee_id: employeeId,
        work_date: { gte: today, lt: tomorrow },
      },
    });

    if (!record) {
      throw new ConflictException('No check-in record found for today');
    }
    if (record.check_out) {
      throw new ConflictException('Already checked out today');
    }

    const now = new Date();
    let workHours = 0;
    let overtimeHours = 0;

    if (record.check_in) {
      const diffMs = now.getTime() - record.check_in.getTime();
      workHours = Math.round((diffMs / (1000 * 60 * 60)) * 10) / 10;
      overtimeHours = workHours > 8 ? Math.round((workHours - 8) * 10) / 10 : 0;
    }

    const updated = await this.prisma.erp_attendance.update({
      where: { id: record.id },
      data: {
        check_out: now,
        work_hours: workHours,
        overtime_hours: overtimeHours,
      },
      include: this.includeRelations(),
    });
    return this.mapAttendance(updated);
  }

  async updateAttendance(id: string, dto: UpdateAttendanceDto) {
    const existing = await this.prisma.erp_attendance.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Attendance not found: ${id}`);

    const updated = await this.prisma.erp_attendance.update({
      where: { id },
      data: {
        work_date: dto.workDate ? parseSafeDate(dto.workDate) : undefined,
        check_in: dto.checkIn ? parseSafeDate(dto.checkIn) : undefined,
        check_out: dto.checkOut ? parseSafeDate(dto.checkOut) : undefined,
        work_hours: dto.workHours,
        overtime_hours: dto.overtimeHours,
        status: dto.status,
        memo: dto.memo,
      },
      include: this.includeRelations(),
    });
    return this.mapAttendance(updated);
  }

  async deleteAttendance(id: string) {
    const existing = await this.prisma.erp_attendance.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Attendance not found: ${id}`);
    await this.prisma.erp_attendance.delete({ where: { id } });
  }
}
