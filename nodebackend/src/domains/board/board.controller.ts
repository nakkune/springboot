import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Request } from '@nestjs/common';
import { BoardService } from './board.service';
import { CreateBoardDto } from './dto/create-board.dto';
import { UpdateBoardDto } from './dto/update-board.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller()
export class BoardController {
  constructor(private readonly boardService: BoardService) {}

  @Post('workspaces/:workspaceId/boards')
  create(
    @Param('workspaceId') workspaceId: string,
    @Body() createBoardDto: CreateBoardDto,
    @Request() req: any
  ) {
    return this.boardService.create(workspaceId, createBoardDto, req.user.id);
  }

  @Get('workspaces/:workspaceId/boards')
  findAll(
    @Param('workspaceId') workspaceId: string,
    @Request() req: any
  ) {
    return this.boardService.findAll(workspaceId, req.user.id);
  }

  @Get('boards/project/:projectId')
  findAllByProject(@Param('projectId') projectId: string) {
    return this.boardService.findAllByProject(projectId);
  }

  @Get('boards/:id')
  findOne(
    @Param('id') id: string,
    @Request() req: any
  ) {
    return this.boardService.findOneById(id);
  }

  @Patch('boards/:id')
  update(
    @Param('id') id: string,
    @Body() updateBoardDto: UpdateBoardDto,
    @Request() req: any
  ) {
    // For simplicity, we just pass empty string as workspaceId if not needed, or update the service.
    return this.boardService.update('', id, updateBoardDto, req.user.id);
  }

  @Delete('boards/:id')
  remove(
    @Param('id') id: string,
    @Request() req: any
  ) {
    return this.boardService.remove('', id, req.user.id);
  }
}
