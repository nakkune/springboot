import { Module } from '@nestjs/common';
import { ErpEmployeeController } from './erp-employee.controller';
import { ErpEmployeeService } from './erp-employee.service';

@Module({
  controllers: [ErpEmployeeController],
  providers: [ErpEmployeeService]
})
export class ErpEmployeeModule {}
