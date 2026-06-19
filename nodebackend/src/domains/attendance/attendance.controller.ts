import { Controller, Get, Post, Put, Delete, Param, Body, Query, HttpCode, HttpStatus } from '@nestjs/common';
import { AttendanceService } from './attendance.service';
import { CreateAttendanceDto, UpdateAttendanceDto, CheckInDto, CheckOutDto } from './dto/attendance.dto';

@Controller('erp/hr/attendance')
export class AttendanceController {
  constructor(private readonly attendanceService: AttendanceService) {}

  @Get()
  getAttendance(
    @Query('employeeId') employeeId?: string,
    @Query('fromDate') fromDate?: string,
    @Query('toDate') toDate?: string,
    @Query('status') status?: string
  ) {
    return this.attendanceService.getAttendance(employeeId, fromDate, toDate, status);
  }

  @Get(':id')
  getAttendanceById(@Param('id') id: string) {
    return this.attendanceService.getAttendanceById(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  createAttendance(@Body() dto: CreateAttendanceDto) {
    return this.attendanceService.createAttendance(dto);
  }

  @Post('check-in')
  @HttpCode(HttpStatus.CREATED)
  checkIn(@Body() body: CheckInDto) {
    return this.attendanceService.checkIn(body.employeeId);
  }

  @Post('check-out')
  checkOut(@Body() body: CheckOutDto) {
    return this.attendanceService.checkOut(body.employeeId);
  }

  @Put(':id')
  updateAttendance(@Param('id') id: string, @Body() dto: UpdateAttendanceDto) {
    return this.attendanceService.updateAttendance(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteAttendance(@Param('id') id: string) {
    return this.attendanceService.deleteAttendance(id);
  }
}
