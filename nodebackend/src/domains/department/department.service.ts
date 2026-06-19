import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';

@Injectable()
export class DepartmentService {
  constructor(private readonly prisma: PrismaService) {}

  private mapDepartment(dept: any) {
    return {
      id: dept.id,
      parentId: dept.parent_id,
      name: dept.name,
      code: dept.code,
      managerId: dept.manager_id,
      sortOrder: Number(dept.sort_order || 0),
      isActive: dept.is_active,
      createdAt: dept.created_at,
      updatedAt: dept.updated_at,
    };
  }

  async getDepartments() {
    const depts = await this.prisma.erp_departments.findMany({
      orderBy: { created_at: 'asc' }
    });
    return depts.map(d => this.mapDepartment(d));
  }

  async getDepartmentTree() {
    const depts = await this.prisma.erp_departments.findMany({
      orderBy: { created_at: 'asc' }
    });

    const map = new Map();
    const roots: any[] = [];

    depts.forEach(d => {
      map.set(d.id, { ...this.mapDepartment(d), children: [] });
    });

    depts.forEach(d => {
      if (d.parent_id && map.has(d.parent_id)) {
        map.get(d.parent_id).children.push(map.get(d.id));
      } else {
        roots.push(map.get(d.id));
      }
    });

    return roots;
  }

  async getDepartment(id: string) {
    const dept = await this.prisma.erp_departments.findUnique({ where: { id } });
    if (!dept) throw new NotFoundException(`Department not found: ${id}`);
    return this.mapDepartment(dept);
  }

  async createDepartment(dto: any) {
    const created = await this.prisma.erp_departments.create({
      data: {
        parent_id: dto.parentId || null,
        name: dto.name,
        code: dto.code,
        manager_id: dto.managerId || null,
        sort_order: dto.sortOrder ?? 0,
        is_active: dto.isActive ?? true,
      }
    });
    return this.mapDepartment(created);
  }

  async updateDepartment(id: string, dto: any) {
    const existing = await this.prisma.erp_departments.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Department not found: ${id}`);

    const updated = await this.prisma.erp_departments.update({
      where: { id },
      data: {
        parent_id: dto.parentId,
        name: dto.name,
        code: dto.code,
        manager_id: dto.managerId,
        sort_order: dto.sortOrder,
        is_active: dto.isActive,
      }
    });
    return this.mapDepartment(updated);
  }

  async deleteDepartment(id: string) {
    const existing = await this.prisma.erp_departments.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Department not found: ${id}`);

    // 하위 부서가 있으면 삭제 불가 (backend 동일 로직)
    const children = await this.prisma.erp_departments.findMany({
      where: { parent_id: id }
    });
    if (children.length > 0) {
      throw new ConflictException('Cannot delete department with child departments. Delete children first.');
    }

    await this.prisma.erp_departments.delete({ where: { id } });
  }
}
