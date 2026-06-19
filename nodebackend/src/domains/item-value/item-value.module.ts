import { Module } from '@nestjs/common';
import { ItemValueController } from './item-value.controller';
import { ItemValueService } from './item-value.service';

@Module({
  controllers: [ItemValueController],
  providers: [ItemValueService]
})
export class ItemValueModule {}
