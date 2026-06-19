import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateErpEmployeeDto } from './dto/create-erp-employee.dto';
import { UpdateErpEmployeeDto } from './dto/update-erp-employee.dto';

@Injectable()
export class ErpEmployeeService {
  constructor(private readonly prisma: PrismaService) {}

  private mapEmployee(item: any) {
    return {
      id: item.id,
      userId: item.user_id,
      employeeName: item.users_erp_employees_user_idTousers?.full_name || '',
      employeeNo: item.employee_no,
      departmentId: item.department_id,
      departmentName: item.erp_departments_erp_employees_department_idToerp_departments?.name || '',
      position: item.position,
      jobTitle: item.job_title,
      employmentType: item.employment_type,
      hireDate: item.hire_date ? item.hire_date.toISOString().split('T')[0] : null,
      resignationDate: item.resignation_date ? item.resignation_date.toISOString().split('T')[0] : null,
      status: item.status,
      phone: item.phone,
      email: item.email || item.users_erp_employees_user_idTousers?.email || '',
      emergencyContact: item.emergency_contact,
      emergencyPhone: item.emergency_phone,
      bankName: item.bank_name,
      bankAccount: item.bank_account,
      annualLeaveDays: Number(item.annual_leave_days || 15),
      leaveStartDate: item.leave_start_date ? item.leave_start_date.toISOString().split('T')[0] : null,
      leaveEndDate: item.leave_end_date ? item.leave_end_date.toISOString().split('T')[0] : null,
      birthDate: item.birth_date ? item.birth_date.toISOString().split('T')[0] : null,
      memo: item.memo,
      createdBy: item.created_by,
      createdAt: item.created_at,
    };
  }

  private includeRelations() {
    return {
      users_erp_employees_user_idTousers: true,
      erp_departments_erp_employees_department_idToerp_departments: true,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.employeeNo !== undefined) dbData.employee_no = dto.employeeNo;
    if (dto.userId !== undefined) dbData.user_id = dto.userId;
    if (dto.departmentId !== undefined) dbData.department_id = dto.departmentId;
    if (dto.position !== undefined) dbData.position = dto.position;
    if (dto.jobTitle !== undefined) dbData.job_title = dto.jobTitle;
    if (dto.employmentType !== undefined) dbData.employment_type = dto.employmentType;
    if (dto.status !== undefined) dbData.status = dto.status;
    if (dto.phone !== undefined) dbData.phone = dto.phone;
    if (dto.email !== undefined) dbData.email = dto.email;
    if (dto.emergencyContact !== undefined) dbData.emergency_contact = dto.emergencyContact;
    if (dto.emergencyPhone !== undefined) dbData.emergency_phone = dto.emergencyPhone;
    if (dto.bankName !== undefined) dbData.bank_name = dto.bankName;
    if (dto.bankAccount !== undefined) dbData.bank_account = dto.bankAccount;
    if (dto.annualLeaveDays !== undefined) dbData.annual_leave_days = dto.annualLeaveDays;
    if (dto.memo !== undefined) dbData.memo = dto.memo;

    if (dto.hireDate) {
      dbData.hire_date = new Date(dto.hireDate);
    }
    if (dto.resignationDate !== undefined) {
      dbData.resignation_date = dto.resignationDate ? new Date(dto.resignationDate) : null;
    }
    if (dto.leaveStartDate !== undefined) {
      dbData.leave_start_date = dto.leaveStartDate ? new Date(dto.leaveStartDate) : null;
    }
    if (dto.leaveEndDate !== undefined) {
      dbData.leave_end_date = dto.leaveEndDate ? new Date(dto.leaveEndDate) : null;
    }
    if (dto.birthDate !== undefined) {
      dbData.birth_date = dto.birthDate ? new Date(dto.birthDate) : null;
    }

    return dbData;
  }

  async create(createErpEmployeeDto: CreateErpEmployeeDto, userId: string) {
    const data = this.toDbModel(createErpEmployeeDto);
    data.created_by = userId;
    const item = await this.prisma.erp_employees.create({
      data,
      include: this.includeRelations(),
    });
    return this.mapEmployee(item);
  }

  async findAll(page: number = 1, size: number = 20, departmentId?: string, status?: string, search?: string) {
    const skip = (page - 1) * size;
    const where: any = {};
    if (departmentId) where.department_id = departmentId;
    if (status) where.status = status;
    if (search) {
      where.OR = [
        { employee_no: { contains: search, mode: 'insensitive' } },
        { users_erp_employees_user_idTousers: { full_name: { contains: search, mode: 'insensitive' } } },
      ];
    }

    const [items, total] = await Promise.all([
      this.prisma.erp_employees.findMany({
        where,
        skip,
        take: size,
        orderBy: { employee_no: 'asc' },
        include: this.includeRelations(),
      }),
      this.prisma.erp_employees.count({ where })
    ]);
    
    return { items: items.map(this.mapEmployee), total };
  }

  async findByUser(userId: string) {
    const employee = await this.prisma.erp_employees.findFirst({
      where: { user_id: userId },
      include: this.includeRelations(),
    });
    if (!employee) {
      throw new NotFoundException(`Employee profile not found for user ${userId}`);
    }
    return this.mapEmployee(employee);
  }

  async findOne(id: string) {
    const employee = await this.prisma.erp_employees.findUnique({
      where: { id },
      include: this.includeRelations(),
    });
    if (!employee) {
      throw new NotFoundException(`Employee ${id} not found`);
    }
    return this.mapEmployee(employee);
  }

  async update(id: string, updateErpEmployeeDto: UpdateErpEmployeeDto) {
    await this.findOne(id);
    const data = this.toDbModel(updateErpEmployeeDto);
    const item = await this.prisma.erp_employees.update({
      where: { id },
      data,
      include: this.includeRelations(),
    });
    return this.mapEmployee(item);
  }

  async updateStatus(id: string, status: string) {
    await this.findOne(id);
    const item = await this.prisma.erp_employees.update({
      where: { id },
      data: { status },
      include: this.includeRelations(),
    });
    return this.mapEmployee(item);
  }

  async remove(id: string) {
    await this.findOne(id);
    return this.prisma.erp_employees.delete({
      where: { id },
    });
  }
}

