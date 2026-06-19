import { Controller, Get, Post, Delete, Query, Param, Body, HttpCode, HttpStatus, Request, UseGuards } from '@nestjs/common';
import { TaxInvoiceService } from './tax-invoice.service';
import { TaxInvoiceDto } from './dto/tax-invoice.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('erp/sales/tax-invoices')
export class TaxInvoiceController {
  constructor(private readonly taxInvoiceService: TaxInvoiceService) {}

  @Get()
  getTaxInvoices(
    @Query('page') page: string = '1',
    @Query('size') size: string = '20'
  ) {
    return this.taxInvoiceService.getTaxInvoices(Number(page), Number(size));
  }

  @Get(':id')
  getTaxInvoiceById(@Param('id') id: string) {
    return this.taxInvoiceService.getTaxInvoiceById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Post()
  @HttpCode(HttpStatus.CREATED)
  saveTaxInvoice(@Body() dto: TaxInvoiceDto, @Request() req: any) {
    const userId = req.user?.id || 'system';
    return this.taxInvoiceService.saveTaxInvoice(dto, userId);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteTaxInvoice(@Param('id') id: string) {
    return this.taxInvoiceService.deleteTaxInvoice(id);
  }
}
