import { Test, TestingModule } from '@nestjs/testing';
import { BoardGroupService } from './board-group.service';

describe('BoardGroupService', () => {
  let service: BoardGroupService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [BoardGroupService],
    }).compile();

    service = module.get<BoardGroupService>(BoardGroupService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
