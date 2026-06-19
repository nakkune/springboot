import { Test, TestingModule } from '@nestjs/testing';
import { ItemValueController } from './item-value.controller';

describe('ItemValueController', () => {
  let controller: ItemValueController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ItemValueController],
    }).compile();

    controller = module.get<ItemValueController>(ItemValueController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
