import { IsString, IsNotEmpty, IsOptional, IsBoolean, IsInt } from 'class-validator';

export class CreateItemDto {
  @IsString()
  @IsNotEmpty()
  groupId: string;

  @IsString()
  @IsOptional()
  parentItemId?: string;

  @IsString()
  @IsNotEmpty()
  name: string;

  @IsInt()
  @IsOptional()
  position?: number;

  @IsBoolean()
  @IsOptional()
  isArchived?: boolean;
}
