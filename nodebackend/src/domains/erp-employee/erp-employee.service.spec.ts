import { Test, TestingModule } from '@nestjs/testing';
import { ErpEmployeeService } from './erp-employee.service';

describe('ErpEmployeeService', () => {
  let service: ErpEmployeeService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ErpEmployeeService],
    }).compile();

    service = module.get<ErpEmployeeService>(ErpEmployeeService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
