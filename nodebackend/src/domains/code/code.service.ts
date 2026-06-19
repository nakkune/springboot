import { Injectable, ConflictException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CodeDto, UpdateCodeDto } from './dto/code.dto';

@Injectable()
export class CodeService {
  constructor(private readonly prisma: PrismaService) {}

  async getGroups() {
    const groups = await this.prisma.erp_codes.groupBy({
      by: ['code_group', 'code_group_name'],
      orderBy: { code_group: 'asc' }
    });
    return groups.map(g => ({
      codeGroup: g.code_group,
      codeGroupName: g.code_group_name || g.code_group 
    }));
  }

  async getCodes(group?: string) {
    const where = group ? { code_group: group } : {};
    const codes = await this.prisma.erp_codes.findMany({
      where,
      orderBy: { sort_order: 'asc' }
    });
    return codes.map(c => this.mapToDto(c));
  }

  async getCode(id: string) {
    const code = await this.prisma.erp_codes.findUnique({ where: { id } });
    if (!code) throw new NotFoundException(`Code with ID ${id} not found`);
    return this.mapToDto(code);
  }

  async createCode(dto: CodeDto) {
    const existing = await this.prisma.erp_codes.findFirst({
      where: { code_group: dto.codeGroup, code: dto.code }
    });

    if (existing) {
      throw new ConflictException(`Code '${dto.code}' already exists in group '${dto.codeGroup}'`);
    }

    const created = await this.prisma.erp_codes.create({
      data: {
        code_group: dto.codeGroup,
        code_group_name: dto.codeGroupName || '',
        code: dto.code,
        label: dto.label,
        sort_order: dto.sortOrder ?? 0,
        is_active: dto.isActive ?? true,
      }
    });

    return this.mapToDto(created);
  }

  async updateCode(id: string, dto: UpdateCodeDto) {
    const existing = await this.prisma.erp_codes.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Code with ID ${id} not found`);

    if (dto.code && dto.code !== existing.code) {
      const groupToCheck = dto.codeGroup || existing.code_group;
      const duplicate = await this.prisma.erp_codes.findFirst({
        where: { code_group: groupToCheck, code: dto.code }
      });
      if (duplicate) {
        throw new ConflictException(`Code '${dto.code}' already exists in group '${groupToCheck}'`);
      }
    }

    const updated = await this.prisma.erp_codes.update({
      where: { id },
      data: {
        code_group: dto.codeGroup,
        code_group_name: dto.codeGroupName,
        code: dto.code,
        label: dto.label,
        sort_order: dto.sortOrder,
        is_active: dto.isActive,
      }
    });

    return this.mapToDto(updated);
  }

  async deleteCode(id: string) {
    const existing = await this.prisma.erp_codes.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Code with ID ${id} not found`);

    await this.prisma.erp_codes.delete({ where: { id } });
  }

  private mapToDto(c: any) {
    return {
      id: c.id,
      codeGroup: c.code_group,
      codeGroupName: c.code_group_name,
      code: c.code,
      label: c.label,
      sortOrder: Number(c.sort_order || 0),
      isActive: c.is_active,
      createdAt: c.created_at,
      updatedAt: c.updated_at,
    };
  }
}
