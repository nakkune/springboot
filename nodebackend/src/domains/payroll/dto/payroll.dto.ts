import { IsString, IsNotEmpty, IsOptional, IsNumber, IsBoolean } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateLedgerDto {
  @IsString()
  @IsNotEmpty()
  title: string;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payYear: number;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payMonth: number;

  @IsString()
  @IsOptional()
  payDate?: string;

  @IsString()
  @IsOptional()
  payType?: string;

  @IsString()
  @IsOptional()
  startDate?: string;

  @IsString()
  @IsOptional()
  endDate?: string;

  @IsString()
  @IsNotEmpty()
  createdBy: string;
}

export class UpdateLedgerItemDto {
  @IsString()
  @IsOptional()
  bankName?: string;

  @IsString()
  @IsOptional()
  bankAccount?: string;

  @IsString()
  @IsOptional()
  bankOwner?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  grossPay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  totalDeduction?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  netPay?: number;

  @IsString()
  @IsOptional()
  memo?: string;
}

export class SavePayrollCodeDto {
  @IsString()
  @IsNotEmpty()
  code: string;

  @IsString()
  @IsNotEmpty()
  name: string;

  @IsString()
  @IsNotEmpty()
  type: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  sortOrder?: number;

  @IsBoolean()
  @IsOptional()
  isActive?: boolean;
}

export class SaveSalaryTemplateDto {
  @IsString()
  @IsNotEmpty()
  employeeId: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  basePay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  positionPay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  mealAllowance?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  carAllowance?: number;

  @IsBoolean()
  @IsOptional()
  useNationalPension?: boolean;

  @IsBoolean()
  @IsOptional()
  useHealthInsurance?: boolean;

  @IsBoolean()
  @IsOptional()
  useEmploymentInsurance?: boolean;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  incomeTaxRate?: number;
}

export class CreatePayrollDto {
  @IsString()
  @IsNotEmpty()
  employeeId: string;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payYear: number;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payMonth: number;

  @IsString()
  @IsNotEmpty()
  payDate: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  basePay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  positionPay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  overtimePay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  bonusPay?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  mealAllowance?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  transportation?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  incomeTax?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  localTax?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  nationalPension?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  healthInsurance?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  employmentInsurance?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  longtermCare?: number;

  @IsString()
  @IsOptional()
  status?: string;

  @IsString()
  @IsNotEmpty()
  createdBy: string;
}

export class BulkCreatePayrollDto {
  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payYear: number;

  @IsNumber()
  @IsNotEmpty()
  @Type(() => Number)
  payMonth: number;

  @IsString()
  @IsNotEmpty()
  payDate: string;
}

export class ConfirmPayrollDto {
  @IsString()
  @IsNotEmpty()
  confirmedBy: string;
}
