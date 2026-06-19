import { Controller, Get, Param, Post, Put, Delete, Body, UseGuards } from '@nestjs/common';
import { BoardColumnService } from './board-column.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CreateBoardColumnDto, UpdateBoardColumnDto } from './dto/board-column.dto';

@UseGuards(JwtAuthGuard)
@Controller('board-columns')
export class BoardColumnController {
  constructor(private readonly boardColumnService: BoardColumnService) {}

  @Get('board/:boardId')
  findAllByBoard(@Param('boardId') boardId: string) {
    return this.boardColumnService.findAllByBoard(boardId);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.boardColumnService.findOne(id);
  }

  @Post()
  create(@Body() createBoardColumnDto: CreateBoardColumnDto) {
    return this.boardColumnService.create(createBoardColumnDto);
  }

  @Put(':id')
  update(@Param('id') id: string, @Body() updateBoardColumnDto: UpdateBoardColumnDto) {
    return this.boardColumnService.update(id, updateBoardColumnDto);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.boardColumnService.remove(id);
  }
}
