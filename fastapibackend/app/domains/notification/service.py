import json
import asyncio
from typing import Dict, List, Optional, Any
from uuid import UUID, uuid4
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import func, update

from app.domains.extra.models import Notification
from app.domains.user.models import User

# In-memory pub/sub broker for SSE
class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[UUID, List[asyncio.Queue]] = {}

    def subscribe(self, user_id: UUID) -> asyncio.Queue:
        queue = asyncio.Queue()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(queue)
        return queue

    def disconnect(self, user_id: UUID, queue: asyncio.Queue):
        if user_id in self.active_connections:
            if queue in self.active_connections[user_id]:
                self.active_connections[user_id].remove(queue)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]

    async def broadcast_to_user(self, user_id: UUID, event_type: str, data: Any):
        if user_id in self.active_connections:
            payload = {
                "event": event_type,
                "data": data
            }
            for queue in self.active_connections[user_id]:
                await queue.put(payload)

manager = ConnectionManager()

# =============================================================
# ── Notification DB 서비스                                   ──
# =============================================================

async def create_notification(
    db: AsyncSession,
    recipient_id: UUID,
    sender_id: Optional[UUID],
    type: str,
    title: str,
    body: Optional[str] = None,
    ref_type: Optional[str] = None,
    ref_id: Optional[UUID] = None
) -> Notification:
    db_obj = Notification(
        id=uuid4(),
        recipient_id=recipient_id,
        sender_id=sender_id,
        type=type,
        title=title,
        body=body,
        ref_type=ref_type,
        ref_id=ref_id,
        is_read=False
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    
    # SSE 브로드캐스트
    # 프론트엔드가 기대하는 카멜케이스 등으로 변환해서 푸시
    # (Pydantic schemas.py가 있으면 좋지만 direct dict로 변환해도 무방)
    payload = {
        "id": str(db_obj.id),
        "recipientId": str(db_obj.recipient_id),
        "senderId": str(db_obj.sender_id) if db_obj.sender_id else None,
        "type": db_obj.type,
        "title": db_obj.title,
        "body": db_obj.body,
        "refType": db_obj.ref_type,
        "refId": str(db_obj.ref_id) if db_obj.ref_id else None,
        "isRead": db_obj.is_read,
        "createdAt": db_obj.created_at.isoformat() if db_obj.created_at else None
    }
    
    await manager.broadcast_to_user(recipient_id, "notification", payload)
    return db_obj

async def get_notifications(db: AsyncSession, user_id: UUID) -> List[Notification]:
    query = select(Notification).filter(Notification.recipient_id == user_id).order_by(Notification.created_at.desc())
    result = await db.execute(query)
    notifications = list(result.scalars().all())
    
    # Response model mapping helper (CamelCase)
    # Pydantic schema를 라우터 response_model로 쓰는 것이 이상적이므로 schemas.py에 매핑해 주면 좋습니다.
    return notifications

async def mark_as_read(db: AsyncSession, id: UUID) -> None:
    await db.execute(
        update(Notification)
        .where(Notification.id == id)
        .values(is_read=True)
    )
    await db.commit()

async def get_unread_count(db: AsyncSession, user_id: UUID) -> int:
    query = select(func.count(Notification.id)).filter(Notification.recipient_id == user_id, Notification.is_read == False)
    result = await db.execute(query)
    return result.scalar() or 0
