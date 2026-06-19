import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateItemValueDto } from './dto/create-item-value.dto';
import { UpdateItemValueDto } from './dto/update-item-value.dto';
import { parseSafeDate } from '../../common/utils/date';

@Injectable()
export class ItemValueService {
  constructor(private readonly prisma: PrismaService) {}

  private mapItemValue(val: any) {
    return {
      id: val.id,
      itemId: val.item_id,
      columnId: val.column_id,
      valueText: val.value_text,
      valueNumber: val.value_number ? Number(val.value_number) : null,
      valueDate: val.value_date ? val.value_date.toISOString().split('T')[0] : null,
      valueJson: val.value_json,
      updatedBy: val.updated_by,
      updatedAt: val.updated_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.columnId !== undefined) dbData.column_id = dto.columnId;
    if (dto.valueText !== undefined) dbData.value_text = dto.valueText;
    if (dto.valueNumber !== undefined) dbData.value_number = dto.valueNumber;
    if (dto.valueDate !== undefined) dbData.value_date = dto.valueDate ? parseSafeDate(dto.valueDate) : null;
    if (dto.valueJson !== undefined) dbData.value_json = dto.valueJson;
    return dbData;
  }

  async create(itemId: string, createItemValueDto: CreateItemValueDto, userId: string) {
    const item = await this.prisma.items.findUnique({
      where: { id: itemId },
    });

    if (!item) {
      throw new NotFoundException(`Item not found.`);
    }

    const data = this.toDbModel(createItemValueDto);
    data.item_id = itemId;
    data.updated_by = userId;

    const val = await this.prisma.item_values.create({
      data,
    });
    return this.mapItemValue(val);
  }

  async findAll(itemId: string) {
    const vals = await this.prisma.item_values.findMany({
      where: {
        item_id: itemId,
      },
    });
    return vals.map(v => this.mapItemValue(v));
  }

  async findAllByBoard(boardId: string) {
    const vals = await this.prisma.item_values.findMany({
      where: {
        items: {
          board_id: boardId
        }
      }
    });
    return vals.map(v => this.mapItemValue(v));
  }

  async findOne(itemId: string, id: string) {
    const itemValue = await this.prisma.item_values.findFirst({
      where: {
        id,
        item_id: itemId,
      },
    });

    if (!itemValue) {
      throw new NotFoundException(`Item value with ID ${id} not found.`);
    }

    return this.mapItemValue(itemValue);
  }

  async update(itemId: string, id: string, updateItemValueDto: UpdateItemValueDto, userId: string) {
    await this.findOne(itemId, id);
    
    const data = this.toDbModel(updateItemValueDto);
    data.updated_by = userId;
    data.updated_at = new Date();

    const val = await this.prisma.item_values.update({
      where: { id },
      data,
    });
    return this.mapItemValue(val);
  }

  async remove(itemId: string, id: string) {
    await this.findOne(itemId, id);
    
    return this.prisma.item_values.delete({
      where: { id },
    });
  }
}
