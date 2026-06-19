import { Controller, Get, Post, Delete, Query, Param, Body, HttpCode, HttpStatus, Request, UseGuards } from '@nestjs/common';
import { QuotationService } from './quotation.service';
import { QuotationDto } from './dto/quotation.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('erp/sales/quotations')
export class QuotationController {
  constructor(private readonly quotationService: QuotationService) {}

  @Get()
  getQuotations(
    @Query('page') page: string = '1',
    @Query('size') size: string = '20'
  ) {
    return this.quotationService.getQuotations(Number(page), Number(size));
  }

  @Get(':id')
  getQuotationById(@Param('id') id: string) {
    return this.quotationService.getQuotationById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Post()
  @HttpCode(HttpStatus.CREATED)
  saveQuotation(@Body() dto: QuotationDto, @Request() req: any) {
    const userId = req.user?.id || 'system';
    return this.quotationService.saveQuotation(dto, userId);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteQuotation(@Param('id') id: string) {
    return this.quotationService.deleteQuotation(id);
  }
}
