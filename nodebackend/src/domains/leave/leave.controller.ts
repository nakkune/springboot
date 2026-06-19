import { Controller, Get, Post, Put, Delete, Param, Body, Query, HttpCode, HttpStatus } from '@nestjs/common';
import { LeaveService } from './leave.service';
import { CreateLeaveDto, UpdateLeaveDto } from './dto/leave.dto';

@Controller('erp/hr/leaves')
export class LeaveController {
  constructor(private readonly leaveService: LeaveService) {}

  @Get()
  getLeaves(
    @Query('employeeId') employeeId?: string,
    @Query('managerId') managerId?: string,
    @Query('status') status?: string
  ) {
    return this.leaveService.getLeaves(employeeId, managerId, status);
  }

  @Get('balance')
  getLeaveBalance(
    @Query('employeeId') employeeId: string,
    @Query('year') year?: string
  ) {
    const targetYear = year ? Number(year) : new Date().getFullYear();
    return this.leaveService.getLeaveBalance(employeeId, targetYear);
  }

  @Get(':id')
  getLeave(@Param('id') id: string) {
    return this.leaveService.getLeave(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  createLeave(@Body() dto: CreateLeaveDto) {
    return this.leaveService.createLeave(dto);
  }

  @Put(':id')
  updateLeave(@Param('id') id: string, @Body() dto: UpdateLeaveDto) {
    return this.leaveService.updateLeave(id, dto);
  }

  @Post(':id/approve')
  approveLeave(@Param('id') id: string, @Body() body: { approverId: string }) {
    return this.leaveService.approveLeave(id, body.approverId);
  }

  @Post(':id/reject')
  rejectLeave(@Param('id') id: string, @Body() body: { approverId: string; rejectReason?: string }) {
    return this.leaveService.rejectLeave(id, body.approverId, body.rejectReason);
  }

  @Post(':id/cancel')
  cancelLeave(@Param('id') id: string) {
    return this.leaveService.cancelLeave(id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteLeave(@Param('id') id: string) {
    return this.leaveService.deleteLeave(id);
  }
}
