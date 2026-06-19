import { Controller, Get, Param, Res, Sse, MessageEvent } from '@nestjs/common';
import { Observable, interval, map } from 'rxjs';
import { NotificationService } from './notification.service';

@Controller('notifications')
export class NotificationController {
  constructor(private readonly notificationService: NotificationService) {}

  @Get('user/:userId')
  getNotifications(@Param('userId') userId: string) {
    return this.notificationService.getNotifications(userId);
  }

  @Get('user/:userId/unread-count')
  getUnreadCount(@Param('userId') userId: string) {
    return this.notificationService.getUnreadCount(userId);
  }

  @Sse('subscribe/:userId')
  subscribe(@Param('userId') userId: string): Observable<MessageEvent> {
    return interval(10000).pipe(map((_) => ({ data: { message: 'keepalive' } } as MessageEvent)));
  }
}
