import { Module } from '@nestjs/common';
import { BoardColumnController } from './board-column.controller';
import { BoardColumnService } from './board-column.service';

@Module({
  controllers: [BoardColumnController],
  providers: [BoardColumnService]
})
export class BoardColumnModule {}
