import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Request } from '@nestjs/common';
import { ItemValueService } from './item-value.service';
import { CreateItemValueDto } from './dto/create-item-value.dto';
import { UpdateItemValueDto } from './dto/update-item-value.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller()
export class ItemValueController {
  constructor(private readonly itemValueService: ItemValueService) {}

  @Post('items/:itemId/values')
  create(
    @Param('itemId') itemId: string,
    @Body() createItemValueDto: CreateItemValueDto,
    @Request() req: any
  ) {
    return this.itemValueService.create(itemId, createItemValueDto, req.user.id);
  }

  @Get('items/:itemId/values')
  findAll(
    @Param('itemId') itemId: string
  ) {
    return this.itemValueService.findAll(itemId);
  }

  @Get('item-values/board/:boardId')
  findAllByBoard(@Param('boardId') boardId: string) {
    return this.itemValueService.findAllByBoard(boardId);
  }

  @Get('items/:itemId/values/:id')
  findOne(
    @Param('itemId') itemId: string,
    @Param('id') id: string
  ) {
    return this.itemValueService.findOne(itemId, id);
  }

  @Patch('items/:itemId/values/:id')
  update(
    @Param('itemId') itemId: string,
    @Param('id') id: string,
    @Body() updateItemValueDto: UpdateItemValueDto,
    @Request() req: any
  ) {
    return this.itemValueService.update(itemId, id, updateItemValueDto, req.user.id);
  }

  @Delete('items/:itemId/values/:id')
  remove(
    @Param('itemId') itemId: string,
    @Param('id') id: string
  ) {
    return this.itemValueService.remove(itemId, id);
  }
}
