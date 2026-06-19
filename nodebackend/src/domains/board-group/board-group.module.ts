import { Module } from '@nestjs/common';
import { BoardGroupController } from './board-group.controller';
import { BoardGroupService } from './board-group.service';

@Module({
  controllers: [BoardGroupController],
  providers: [BoardGroupService]
})
export class BoardGroupModule {}
