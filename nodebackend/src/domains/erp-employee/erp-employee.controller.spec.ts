import { Test, TestingModule } from '@nestjs/testing';
import { ErpEmployeeController } from './erp-employee.controller';

describe('ErpEmployeeController', () => {
  let controller: ErpEmployeeController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ErpEmployeeController],
    }).compile();

    controller = module.get<ErpEmployeeController>(ErpEmployeeController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
