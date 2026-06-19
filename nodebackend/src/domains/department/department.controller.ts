import { Controller, Get, Post, Put, Delete, Param, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { DepartmentService } from './department.service';

@Controller('erp/hr/departments')
export class DepartmentController {
  constructor(private readonly departmentService: DepartmentService) {}

  @Get()
  getDepartments() {
    return this.departmentService.getDepartments();
  }

  @Get('tree')
  getDepartmentTree() {
    return this.departmentService.getDepartmentTree();
  }

  @Get(':id')
  getDepartment(@Param('id') id: string) {
    return this.departmentService.getDepartment(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  createDepartment(@Body() dto: any) {
    return this.departmentService.createDepartment(dto);
  }

  @Put(':id')
  updateDepartment(@Param('id') id: string, @Body() dto: any) {
    return this.departmentService.updateDepartment(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteDepartment(@Param('id') id: string) {
    return this.departmentService.deleteDepartment(id);
  }
}
