import { IsString, IsNotEmpty, IsOptional, IsNumber, IsArray, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

export class TaxInvoiceItemDto {
  @IsString()
  @IsOptional()
  id?: string;

  @IsString()
  @IsOptional()
  taxInvoiceId?: string;

  @IsString()
  @IsNotEmpty()
  itemDate: string; // MM-DD

  @IsString()
  @IsNotEmpty()
  itemName: string;

  @IsString()
  @IsOptional()
  spec?: string;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  qty: number;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  unitPrice: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  supplyValue?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  taxValue?: number;

  @IsString()
  @IsOptional()
  remarks?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  sortOrder?: number;
}

export class TaxInvoiceDto {
  @IsString()
  @IsOptional()
  id?: string;

  @IsString()
  @IsOptional()
  issueId?: string;

  @IsString()
  @IsNotEmpty()
  writeDate: string;

  @IsString()
  @IsNotEmpty()
  supplierRegNo: string;

  @IsString()
  @IsOptional()
  supplierSubNo?: string;

  @IsString()
  @IsNotEmpty()
  supplierName: string;

  @IsString()
  @IsNotEmpty()
  supplierCeo: string;

  @IsString()
  @IsOptional()
  supplierAddress?: string;

  @IsString()
  @IsOptional()
  supplierBizType?: string;

  @IsString()
  @IsOptional()
  supplierBizItem?: string;

  @IsString()
  @IsOptional()
  supplierEmail?: string;

  @IsString()
  @IsNotEmpty()
  customerRegNo: string;

  @IsString()
  @IsOptional()
  customerSubNo?: string;

  @IsString()
  @IsNotEmpty()
  customerName: string;

  @IsString()
  @IsOptional()
  customerCeo?: string;

  @IsString()
  @IsOptional()
  customerAddress?: string;

  @IsString()
  @IsOptional()
  customerBizType?: string;

  @IsString()
  @IsOptional()
  customerBizItem?: string;

  @IsString()
  @IsOptional()
  customerEmail1?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  totalSupplyValue?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  totalTaxValue?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  totalAmount?: number;

  @IsString()
  @IsOptional()
  purpose?: string; // 'charge' | 'receipt'

  @IsString()
  @IsOptional()
  status?: string;

  @IsString()
  @IsOptional()
  createdBy?: string;

  @IsArray()
  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => TaxInvoiceItemDto)
  items?: TaxInvoiceItemDto[];
}
