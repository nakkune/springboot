import { PartialType } from '@nestjs/mapped-types';
import { CreateItemValueDto } from './create-item-value.dto';

export class UpdateItemValueDto extends PartialType(CreateItemValueDto) {}
