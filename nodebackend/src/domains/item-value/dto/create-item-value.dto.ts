import { IsString, IsNotEmpty, IsOptional, IsNumber, IsDateString } from 'class-validator';

export class CreateItemValueDto {
  @IsString()
  @IsNotEmpty()
  columnId: string;

  @IsString()
  @IsOptional()
  valueText?: string;

  @IsNumber()
  @IsOptional()
  valueNumber?: number;

  @IsDateString()
  @IsOptional()
  valueDate?: string;

  @IsOptional()
  valueJson?: any;
}
