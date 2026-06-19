import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Request } from '@nestjs/common';
import { ItemService } from './item.service';
import { CreateItemDto } from './dto/create-item.dto';
import { UpdateItemDto } from './dto/update-item.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller()
export class ItemController {
  constructor(private readonly itemService: ItemService) {}

  @Post('boards/:boardId/items')
  create(
    @Param('boardId') boardId: string,
    @Body() createItemDto: CreateItemDto,
    @Request() req: any
  ) {
    return this.itemService.create(boardId, createItemDto, req.user.id);
  }

  @Get('boards/:boardId/items')
  findAll(
    @Param('boardId') boardId: string
  ) {
    return this.itemService.findAll(boardId);
  }

  @Get('items/board/:boardId')
  findAllByBoard(@Param('boardId') boardId: string) {
    return this.itemService.findAll(boardId);
  }

  @Get('boards/:boardId/items/:id')
  findOne(
    @Param('boardId') boardId: string,
    @Param('id') id: string
  ) {
    return this.itemService.findOne(boardId, id);
  }

  @Patch('boards/:boardId/items/:id')
  update(
    @Param('boardId') boardId: string,
    @Param('id') id: string,
    @Body() updateItemDto: UpdateItemDto,
    @Request() req: any
  ) {
    return this.itemService.update(boardId, id, updateItemDto, req.user.id);
  }

  @Delete('boards/:boardId/items/:id')
  remove(
    @Param('boardId') boardId: string,
    @Param('id') id: string
  ) {
    return this.itemService.remove(boardId, id);
  }
}
