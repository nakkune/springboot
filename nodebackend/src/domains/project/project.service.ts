import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateProjectDto } from './dto/create-project.dto';
import { UpdateProjectDto } from './dto/update-project.dto';

@Injectable()
export class ProjectService {
  constructor(private readonly prisma: PrismaService) {}

  private mapProject(p: any) {
    return {
      id: p.id,
      workspaceId: p.workspace_id,
      teamId: p.team_id,
      name: p.name,
      description: p.description,
      color: p.color,
      icon: p.icon,
      isArchived: p.is_archived,
      createdBy: p.created_by,
      createdAt: p.created_at,
      updatedAt: p.updated_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.name !== undefined) dbData.name = dto.name;
    if (dto.description !== undefined) dbData.description = dto.description;
    if (dto.color !== undefined) dbData.color = dto.color;
    if (dto.icon !== undefined) dbData.icon = dto.icon;
    if (dto.workspaceId !== undefined) dbData.workspace_id = dto.workspaceId;
    if (dto.teamId !== undefined) dbData.team_id = dto.teamId;
    if (dto.isArchived !== undefined) dbData.is_archived = dto.isArchived;
    return dbData;
  }

  async create(createProjectDto: CreateProjectDto, userId: string) {
    const data = this.toDbModel(createProjectDto);
    data.created_by = userId;
    const project = await this.prisma.projects.create({
      data,
    });
    return this.mapProject(project);
  }

  async findAllByWorkspace(workspaceId: string) {
    const projects = await this.prisma.projects.findMany({
      where: { workspace_id: workspaceId },
      orderBy: { created_at: 'asc' },
    });
    return projects.map(p => this.mapProject(p));
  }

  async findOne(id: string) {
    const project = await this.prisma.projects.findUnique({
      where: { id },
    });
    if (!project) {
      throw new NotFoundException(`Project ${id} not found`);
    }
    return this.mapProject(project);
  }

  async update(id: string, updateProjectDto: UpdateProjectDto) {
    await this.findOne(id);
    const data = this.toDbModel(updateProjectDto);
    const project = await this.prisma.projects.update({
      where: { id },
      data,
    });
    return this.mapProject(project);
  }

  async remove(id: string) {
    await this.findOne(id);
    return this.prisma.projects.delete({
      where: { id },
    });
  }
}
