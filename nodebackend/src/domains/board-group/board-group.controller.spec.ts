import { Test, TestingModule } from '@nestjs/testing';
import { BoardGroupController } from './board-group.controller';

describe('BoardGroupController', () => {
  let controller: BoardGroupController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [BoardGroupController],
    }).compile();

    controller = module.get<BoardGroupController>(BoardGroupController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
