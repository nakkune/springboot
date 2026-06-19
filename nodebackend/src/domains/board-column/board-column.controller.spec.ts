import { Test, TestingModule } from '@nestjs/testing';
import { BoardColumnController } from './board-column.controller';

describe('BoardColumnController', () => {
  let controller: BoardColumnController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [BoardColumnController],
    }).compile();

    controller = module.get<BoardColumnController>(BoardColumnController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
