import { IsString, IsNotEmpty, IsOptional } from 'class-validator';

export class CreateActivityLogDto {
  @IsString()
  @IsNotEmpty()
  action: string;

  @IsString()
  @IsOptional()
  workspaceId?: string;

  @IsString()
  @IsOptional()
  projectId?: string;

  @IsString()
  @IsOptional()
  boardId?: string;

  @IsString()
  @IsOptional()
  itemId?: string;

  @IsOptional()
  meta?: any;
}
