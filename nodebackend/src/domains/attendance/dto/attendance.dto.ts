import { IsString, IsNotEmpty, IsOptional, IsNumber, IsDateString } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateAttendanceDto {
  @IsString()
  @IsNotEmpty()
  employeeId: string;

  @IsString()
  @IsOptional()
  workDate?: string;

  @IsString()
  @IsOptional()
  checkIn?: string;

  @IsString()
  @IsOptional()
  checkOut?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  workHours?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  overtimeHours?: number;

  @IsString()
  @IsOptional()
  status?: string;

  @IsString()
  @IsOptional()
  memo?: string;
}

export class UpdateAttendanceDto {
  @IsString()
  @IsOptional()
  workDate?: string;

  @IsString()
  @IsOptional()
  checkIn?: string;

  @IsString()
  @IsOptional()
  checkOut?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  workHours?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  overtimeHours?: number;

  @IsString()
  @IsOptional()
  status?: string;

  @IsString()
  @IsOptional()
  memo?: string;
}

export class CheckInDto {
  @IsString()
  @IsNotEmpty()
  employeeId: string;
}

export class CheckOutDto {
  @IsString()
  @IsNotEmpty()
  employeeId: string;
}
