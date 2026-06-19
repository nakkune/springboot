import { Test, TestingModule } from '@nestjs/testing';
import { TaxInvoiceService } from './tax-invoice.service';

describe('TaxInvoiceService', () => {
  let service: TaxInvoiceService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [TaxInvoiceService],
    }).compile();

    service = module.get<TaxInvoiceService>(TaxInvoiceService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
