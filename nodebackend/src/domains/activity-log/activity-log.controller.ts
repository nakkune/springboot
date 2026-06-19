import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Request, Query } from '@nestjs/common';
import { ActivityLogService } from './activity-log.service';
import { CreateActivityLogDto } from './dto/create-activity-log.dto';
import { UpdateActivityLogDto } from './dto/update-activity-log.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('activity-logs')
export class ActivityLogController {
  constructor(private readonly activityLogService: ActivityLogService) {}

  @Post()
  create(@Body() createActivityLogDto: CreateActivityLogDto, @Request() req: any) {
    return this.activityLogService.create(createActivityLogDto, req.user.id);
  }

  @Get('workspace/:workspaceId')
  findAllByWorkspace(@Param('workspaceId') workspaceId: string) {
    return this.activityLogService.findAllByWorkspace(workspaceId);
  }

  @Get('board/:boardId')
  findAllByBoard(@Param('boardId') boardId: string) {
    return this.activityLogService.findAllByBoard(boardId);
  }

  @Get('item/:itemId')
  findAllByItem(@Param('itemId') itemId: string) {
    return this.activityLogService.findAllByItem(itemId);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.activityLogService.findOne(id);
  }

  @Patch(':id')
  update(@Param('id') id: string, @Body() updateActivityLogDto: UpdateActivityLogDto) {
    return this.activityLogService.update(id, updateActivityLogDto);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.activityLogService.remove(id);
  }
}
