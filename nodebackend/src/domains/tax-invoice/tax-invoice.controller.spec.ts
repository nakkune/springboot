import { Test, TestingModule } from '@nestjs/testing';
import { TaxInvoiceController } from './tax-invoice.controller';

describe('TaxInvoiceController', () => {
  let controller: TaxInvoiceController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [TaxInvoiceController],
    }).compile();

    controller = module.get<TaxInvoiceController>(TaxInvoiceController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
