import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateItemDto } from './dto/create-item.dto';
import { UpdateItemDto } from './dto/update-item.dto';

@Injectable()
export class ItemService {
  constructor(private readonly prisma: PrismaService) {}

  private mapItem(item: any) {
    // EAV item_values를 평탄화한 values 맵 생성 (columnId -> resolvedValue)
    const values: Record<string, any> = {};
    if (item.item_values) {
      item.item_values.forEach((val: any) => {
        const resolved = val.value_text || val.value_json || (val.value_date ? val.value_date.toISOString().split('T')[0] : null) || (val.value_number ? Number(val.value_number) : null) || '';
        values[val.column_id] = resolved;
      });
    }

    return {
      id: item.id,
      boardId: item.board_id,
      groupId: item.group_id,
      parentItemId: item.parent_item_id,
      name: item.name,
      position: item.position,
      isArchived: item.is_archived,
      createdBy: item.created_by,
      createdAt: item.created_at,
      updatedAt: item.updated_at,
      values,
      itemValues: item.item_values ? item.item_values.map((val: any) => ({
        id: val.id,
        itemId: val.item_id,
        columnId: val.column_id,
        valueText: val.value_text,
        valueNumber: val.value_number ? Number(val.value_number) : null,
        valueDate: val.value_date ? val.value_date.toISOString().split('T')[0] : null,
        valueJson: val.value_json,
        updatedBy: val.updated_by,
        updatedAt: val.updated_at,
      })) : [],
      comments: item.comments ? item.comments.map((c: any) => ({
        id: c.id,
        itemId: c.item_id,
        parentId: c.parent_id,
        authorId: c.author_id,
        body: c.body,
        isEdited: c.is_edited,
        createdAt: c.created_at,
        updatedAt: c.updated_at,
      })) : [],
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.name !== undefined) dbData.name = dto.name;
    if (dto.position !== undefined) dbData.position = dto.position;
    if (dto.groupId !== undefined) dbData.group_id = dto.groupId;
    if (dto.parentItemId !== undefined) dbData.parent_item_id = dto.parentItemId;
    if (dto.isArchived !== undefined) dbData.is_archived = dto.isArchived;
    return dbData;
  }

  async create(boardId: string, createItemDto: CreateItemDto, userId: string) {
    const board = await this.prisma.boards.findUnique({
      where: { id: boardId },
    });

    if (!board) {
      throw new NotFoundException(`Board not found.`);
    }

    const data = this.toDbModel(createItemDto);
    data.board_id = boardId;
    data.created_by = userId;

    const item = await this.prisma.items.create({
      data,
    });
    return this.mapItem(item);
  }

  async findAll(boardId: string) {
    const items = await this.prisma.items.findMany({
      where: {
        board_id: boardId,
      },
      include: {
        item_values: true,
      },
      orderBy: {
        position: 'asc'
      }
    });
    return items.map(item => this.mapItem(item));
  }

  async findOne(boardId: string, id: string) {
    const item = await this.prisma.items.findFirst({
      where: {
        id,
        board_id: boardId,
      },
      include: {
        item_values: true,
        comments: true,
      }
    });

    if (!item) {
      throw new NotFoundException(`Item with ID ${id} not found.`);
    }

    return this.mapItem(item);
  }

  async update(boardId: string, id: string, updateItemDto: UpdateItemDto, userId: string) {
    await this.findOne(boardId, id);
    
    const data = this.toDbModel(updateItemDto);
    const item = await this.prisma.items.update({
      where: { id },
      data,
      include: {
        item_values: true,
      }
    });
    return this.mapItem(item);
  }

  async remove(boardId: string, id: string) {
    await this.findOne(boardId, id);
    
    return this.prisma.items.delete({
      where: { id },
    });
  }
}
