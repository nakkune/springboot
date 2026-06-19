import { Test, TestingModule } from '@nestjs/testing';
import { ItemValueService } from './item-value.service';

describe('ItemValueService', () => {
  let service: ItemValueService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ItemValueService],
    }).compile();

    service = module.get<ItemValueService>(ItemValueService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
