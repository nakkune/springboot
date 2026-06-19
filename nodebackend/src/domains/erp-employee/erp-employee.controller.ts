import { Controller, Get, Post, Put, Body, Patch, Param, Delete, UseGuards, Request, Query } from '@nestjs/common';
import { ErpEmployeeService } from './erp-employee.service';
import { CreateErpEmployeeDto } from './dto/create-erp-employee.dto';
import { UpdateErpEmployeeDto } from './dto/update-erp-employee.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('erp/hr/employees')
export class ErpEmployeeController {
  constructor(private readonly erpEmployeeService: ErpEmployeeService) {}

  @Post()
  create(@Body() createErpEmployeeDto: CreateErpEmployeeDto, @Request() req: any) {
    return this.erpEmployeeService.create(createErpEmployeeDto, req.user.id);
  }

  @Get()
  findAll(
    @Query('departmentId') departmentId?: string,
    @Query('status') status?: string,
    @Query('search') search?: string,
    @Query('page') page: string = '1',
    @Query('size') size: string = '20',
  ) {
    return this.erpEmployeeService.findAll(
      Number(page),
      Number(size),
      departmentId,
      status,
      search,
    );
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.erpEmployeeService.findOne(id);
  }

  @Get('user/:userId')
  findByUser(@Param('userId') userId: string) {
    return this.erpEmployeeService.findByUser(userId);
  }

  @Put(':id')
  updateEmployee(@Param('id') id: string, @Body() updateErpEmployeeDto: UpdateErpEmployeeDto) {
    return this.erpEmployeeService.update(id, updateErpEmployeeDto);
  }

  @Patch(':id')
  update(@Param('id') id: string, @Body() updateErpEmployeeDto: UpdateErpEmployeeDto) {
    return this.erpEmployeeService.update(id, updateErpEmployeeDto);
  }

  @Patch(':id/status')
  updateStatus(@Param('id') id: string, @Body() body: { status: string }) {
    return this.erpEmployeeService.updateStatus(id, body.status);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.erpEmployeeService.remove(id);
  }
}
