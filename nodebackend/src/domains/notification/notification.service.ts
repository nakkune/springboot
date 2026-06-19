import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';

@Injectable()
export class NotificationService {
  constructor(private readonly prisma: PrismaService) {}

  private mapNotification(n: any) {
    return {
      id: n.id,
      recipientId: n.recipient_id,
      senderId: n.sender_id,
      type: n.type,
      title: n.title,
      body: n.body,
      refType: n.ref_type,
      refId: n.ref_id,
      isRead: n.is_read ?? false,
      createdAt: n.created_at,
    };
  }

  async getNotifications(userId: string) {
    const notifications = await this.prisma.notifications.findMany({
      where: { recipient_id: userId },
      orderBy: { created_at: 'desc' },
      take: 50 // Limit to recent 50 notifications
    });
    return notifications.map(n => this.mapNotification(n));
  }

  async getUnreadCount(userId: string) {
    const count = await this.prisma.notifications.count({
      where: {
        recipient_id: userId,
        is_read: false
      }
    });
    return { count };
  }
}
