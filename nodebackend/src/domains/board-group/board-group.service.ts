import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateBoardGroupDto, UpdateBoardGroupDto } from './dto/board-group.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class BoardGroupService {
  constructor(private readonly prisma: PrismaService) {}

  private mapGroup(g: any) {
    return {
      id: g.id,
      boardId: g.board_id,
      title: g.title,
      color: g.color || '',
      position: g.position ?? 0,
      isCollapsed: g.is_collapsed ?? false,
      createdAt: g.created_at,
      updatedAt: g.updated_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.boardId !== undefined) dbData.board_id = dto.boardId;
    if (dto.title !== undefined) dbData.title = dto.title;
    if (dto.color !== undefined) dbData.color = dto.color;
    if (dto.position !== undefined) dbData.position = dto.position;
    if (dto.isCollapsed !== undefined) dbData.is_collapsed = dto.isCollapsed;
    return dbData;
  }

  async findAllByBoard(boardId: string) {
    const groups = await this.prisma.board_groups.findMany({
      where: { board_id: boardId },
      orderBy: { position: 'asc' }
    });
    return groups.map(g => this.mapGroup(g));
  }

  async findOne(id: string) {
    const group = await this.prisma.board_groups.findUnique({
      where: { id }
    });
    if (!group) {
      throw new NotFoundException(`Board group not found with ID ${id}`);
    }
    return this.mapGroup(group);
  }

  async createBoardGroup(createBoardGroupDto: CreateBoardGroupDto) {
    const data = this.toDbModel(createBoardGroupDto);
    if (!data.id) {
      data.id = randomUUID();
    }
    if (data.position === undefined) {
      data.position = 0;
    }
    if (data.is_collapsed === undefined) {
      data.is_collapsed = false;
    }

    const group = await this.prisma.board_groups.create({
      data
    });
    return this.mapGroup(group);
  }

  async updateBoardGroup(id: string, updateBoardGroupDto: UpdateBoardGroupDto) {
    await this.findOne(id);

    const data = this.toDbModel(updateBoardGroupDto);
    const group = await this.prisma.board_groups.update({
      where: { id },
      data
    });
    return this.mapGroup(group);
  }

  async deleteBoardGroup(id: string) {
    await this.findOne(id);

    await this.prisma.board_groups.delete({
      where: { id }
    });
  }
}
