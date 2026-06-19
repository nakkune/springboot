import { Controller, Get, Post, Body, Put, Param, Delete, UseGuards, Request } from '@nestjs/common';
import { WorkspaceService } from './workspace.service';
import { CreateWorkspaceDto } from './dto/create-workspace.dto';
import { UpdateWorkspaceDto } from './dto/update-workspace.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('workspaces')
export class WorkspaceController {
  constructor(private readonly workspaceService: WorkspaceService) {}

  @Post()
  create(@Body() createWorkspaceDto: CreateWorkspaceDto, @Request() req: any) {
    return this.workspaceService.create(createWorkspaceDto, req.user.id);
  }

  @Get()
  findAll(@Request() req: any) {
    return this.workspaceService.findAll(req.user.id);
  }

  @Get(':id')
  findOne(@Param('id') id: string, @Request() req: any) {
    return this.workspaceService.findOne(id, req.user.id);
  }

  @Get(':id/members')
  getMembers(@Param('id') id: string, @Request() req: any) {
    return this.workspaceService.getMembers(id, req.user.id);
  }

  @Post(':id/members/invite')
  inviteMember(
    @Param('id') workspaceId: string,
    @Body() body: { email: string; role?: string }
  ) {
    return this.workspaceService.inviteMember(workspaceId, body.email, body.role || 'member');
  }

  @Put(':workspaceId/members/:memberId')
  updateMemberRole(
    @Param('workspaceId') workspaceId: string,
    @Param('memberId') memberId: string,
    @Body() body: { role: string }
  ) {
    return this.workspaceService.updateMemberRole(workspaceId, memberId, body.role);
  }

  @Delete(':workspaceId/members/:memberId')
  deleteMember(
    @Param('workspaceId') workspaceId: string,
    @Param('memberId') memberId: string
  ) {
    return this.workspaceService.deleteMember(workspaceId, memberId);
  }

  @Put(':id')
  update(@Param('id') id: string, @Body() updateWorkspaceDto: UpdateWorkspaceDto, @Request() req: any) {
    return this.workspaceService.update(id, updateWorkspaceDto, req.user.id);
  }

  @Delete(':id')
  remove(@Param('id') id: string, @Request() req: any) {
    return this.workspaceService.remove(id, req.user.id);
  }
}
