import { Controller, Get, Post, Put, Delete, Param, Query, Body, HttpCode, HttpStatus, ValidationPipe } from '@nestjs/common';
import { CodeService } from './code.service';
import { CodeDto, UpdateCodeDto } from './dto/code.dto';

@Controller('erp/hr/codes')
export class CodeController {
  constructor(private readonly codeService: CodeService) {}

  @Get('groups')
  getGroups() {
    return this.codeService.getGroups();
  }

  @Get()
  getCodes(@Query('group') group?: string) {
    return this.codeService.getCodes(group);
  }

  @Get(':id')
  getCode(@Param('id') id: string) {
    return this.codeService.getCode(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  createCode(@Body(new ValidationPipe({ whitelist: true })) dto: CodeDto) {
    return this.codeService.createCode(dto);
  }

  @Put(':id')
  updateCode(
    @Param('id') id: string,
    @Body(new ValidationPipe({ whitelist: true })) dto: UpdateCodeDto
  ) {
    return this.codeService.updateCode(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteCode(@Param('id') id: string) {
    return this.codeService.deleteCode(id);
  }
}
