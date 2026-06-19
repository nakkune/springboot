import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateActivityLogDto } from './dto/create-activity-log.dto';
import { UpdateActivityLogDto } from './dto/update-activity-log.dto';

@Injectable()
export class ActivityLogService {
  constructor(private readonly prisma: PrismaService) {}

  private mapLog(log: any) {
    return {
      id: log.id,
      workspaceId: log.workspace_id,
      projectId: log.project_id,
      boardId: log.board_id,
      itemId: log.item_id,
      actorId: log.actor_id,
      action: log.action,
      meta: log.meta,
      createdAt: log.created_at,
      actorName: log.users?.full_name || 'System',
      actorAvatar: log.users?.avatar || '',
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.action !== undefined) dbData.action = dto.action;
    if (dto.workspaceId !== undefined) dbData.workspace_id = dto.workspaceId;
    if (dto.projectId !== undefined) dbData.project_id = dto.projectId;
    if (dto.boardId !== undefined) dbData.board_id = dto.boardId;
    if (dto.itemId !== undefined) dbData.item_id = dto.itemId;
    if (dto.meta !== undefined) dbData.meta = dto.meta;
    return dbData;
  }

  async create(createActivityLogDto: CreateActivityLogDto, userId: string) {
    const data = this.toDbModel(createActivityLogDto);
    data.actor_id = userId;
    const log = await this.prisma.activity_logs.create({
      data,
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    return this.mapLog(log);
  }

  async findAllByWorkspace(workspaceId: string) {
    const logs = await this.prisma.activity_logs.findMany({
      where: { workspace_id: workspaceId },
      orderBy: { created_at: 'desc' },
      take: 50,
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    return logs.map(l => this.mapLog(l));
  }

  async findAllByBoard(boardId: string) {
    const logs = await this.prisma.activity_logs.findMany({
      where: { board_id: boardId },
      orderBy: { created_at: 'desc' },
      take: 50,
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    return logs.map(l => this.mapLog(l));
  }

  async findAllByItem(itemId: string) {
    const logs = await this.prisma.activity_logs.findMany({
      where: { item_id: itemId },
      orderBy: { created_at: 'desc' },
      take: 50,
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    return logs.map(l => this.mapLog(l));
  }

  async findOne(id: string) {
    const log = await this.prisma.activity_logs.findUnique({
      where: { id },
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    if (!log) {
      throw new NotFoundException(`Activity log ${id} not found`);
    }
    return this.mapLog(log);
  }

  async update(id: string, updateActivityLogDto: UpdateActivityLogDto) {
    await this.findOne(id);
    const data = this.toDbModel(updateActivityLogDto);
    const log = await this.prisma.activity_logs.update({
      where: { id },
      data,
      include: {
        users: {
          select: {
            id: true,
            full_name: true,
            email: true,
          }
        }
      }
    });
    return this.mapLog(log);
  }

  async remove(id: string) {
    await this.findOne(id);
    return this.prisma.activity_logs.delete({
      where: { id },
    });
  }
}
