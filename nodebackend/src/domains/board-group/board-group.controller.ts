import { Controller, Get, Param, Post, Put, Delete, Body, UseGuards } from '@nestjs/common';
import { BoardGroupService } from './board-group.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CreateBoardGroupDto, UpdateBoardGroupDto } from './dto/board-group.dto';

@UseGuards(JwtAuthGuard)
@Controller('board-groups')
export class BoardGroupController {
  constructor(private readonly boardGroupService: BoardGroupService) {}

  @Get('board/:boardId')
  findAllByBoard(@Param('boardId') boardId: string) {
    return this.boardGroupService.findAllByBoard(boardId);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.boardGroupService.findOne(id);
  }

  @Post()
  createBoardGroup(@Body() createBoardGroupDto: CreateBoardGroupDto) {
    return this.boardGroupService.createBoardGroup(createBoardGroupDto);
  }

  @Put(':id')
  updateBoardGroup(@Param('id') id: string, @Body() updateBoardGroupDto: UpdateBoardGroupDto) {
    return this.boardGroupService.updateBoardGroup(id, updateBoardGroupDto);
  }

  @Delete(':id')
  deleteBoardGroup(@Param('id') id: string) {
    return this.boardGroupService.deleteBoardGroup(id);
  }
}
