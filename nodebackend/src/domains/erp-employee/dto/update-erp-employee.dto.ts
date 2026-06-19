import { PartialType } from '@nestjs/mapped-types';
import { CreateErpEmployeeDto } from './create-erp-employee.dto';

export class UpdateErpEmployeeDto extends PartialType(CreateErpEmployeeDto) {}
