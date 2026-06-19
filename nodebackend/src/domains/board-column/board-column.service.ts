import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateBoardColumnDto, UpdateBoardColumnDto } from './dto/board-column.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class BoardColumnService {
  constructor(private readonly prisma: PrismaService) {}

  private mapColumn(c: any) {
    return {
      id: c.id,
      boardId: c.board_id,
      name: c.name,
      columnType: c.column_type,
      settings: c.settings ? (typeof c.settings === 'string' ? c.settings : JSON.stringify(c.settings)) : '{}',
      position: c.position ?? 0,
      isRequired: c.is_required ?? false,
      isHidden: c.is_hidden ?? false,
      createdAt: c.created_at,
      updatedAt: c.updated_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.boardId !== undefined) dbData.board_id = dto.boardId;
    if (dto.name !== undefined) dbData.name = dto.name;
    if (dto.columnType !== undefined) dbData.column_type = dto.columnType;
    if (dto.settings !== undefined) {
      if (typeof dto.settings === 'string') {
        try {
          dbData.settings = JSON.parse(dto.settings);
        } catch (e) {
          dbData.settings = {};
        }
      } else {
        dbData.settings = dto.settings;
      }
    }
    if (dto.position !== undefined) dbData.position = dto.position;
    if (dto.isRequired !== undefined) dbData.is_required = dto.isRequired;
    if (dto.isHidden !== undefined) dbData.is_hidden = dto.isHidden;
    return dbData;
  }

  async findAllByBoard(boardId: string) {
    const cols = await this.prisma.board_columns.findMany({
      where: { board_id: boardId },
      orderBy: { position: 'asc' }
    });
    return cols.map(c => this.mapColumn(c));
  }

  async findOne(id: string) {
    const col = await this.prisma.board_columns.findUnique({
      where: { id }
    });
    if (!col) {
      throw new NotFoundException(`Board column not found with ID ${id}`);
    }
    return this.mapColumn(col);
  }

  async create(createBoardColumnDto: CreateBoardColumnDto) {
    const data = this.toDbModel(createBoardColumnDto);
    
    // Add default values if missing
    if (!data.id) {
      data.id = randomUUID();
    }
    if (data.settings === undefined) {
      data.settings = {};
    }
    if (data.position === undefined) {
      data.position = 0;
    }
    if (data.is_required === undefined) {
      data.is_required = false;
    }
    if (data.is_hidden === undefined) {
      data.is_hidden = false;
    }

    const col = await this.prisma.board_columns.create({
      data
    });
    return this.mapColumn(col);
  }

  async update(id: string, updateBoardColumnDto: UpdateBoardColumnDto) {
    // Check if column exists
    await this.findOne(id);

    const data = this.toDbModel(updateBoardColumnDto);
    const col = await this.prisma.board_columns.update({
      where: { id },
      data
    });
    return this.mapColumn(col);
  }

  async remove(id: string) {
    // Check if column exists
    await this.findOne(id);

    await this.prisma.board_columns.delete({
      where: { id }
    });
  }
}
