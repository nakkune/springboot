import { IsString, IsNotEmpty, IsOptional, IsNumber, IsArray, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

export class QuotationItemDto {
  @IsString()
  @IsOptional()
  id?: string;

  @IsString()
  @IsOptional()
  quotationId?: string;

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

export class QuotationDto {
  @IsString()
  @IsOptional()
  id?: string;

  @IsString()
  @IsOptional()
  quotationNo?: string;

  @IsString()
  @IsNotEmpty()
  title: string;

  @IsString()
  @IsNotEmpty()
  quoteDate: string;

  @IsString()
  @IsOptional()
  validDate?: string;

  @IsString()
  @IsNotEmpty()
  supplierRegNo: string;

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
  @IsNotEmpty()
  customerName: string;

  @IsString()
  @IsOptional()
  customerCeo?: string;

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
  remarks?: string;

  @IsString()
  @IsOptional()
  status?: string;

  @IsString()
  @IsOptional()
  createdBy?: string;

  @IsArray()
  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => QuotationItemDto)
  items?: QuotationItemDto[];
}
