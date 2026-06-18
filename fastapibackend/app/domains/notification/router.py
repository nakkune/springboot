import json
import asyncio
from typing import Any, List, Dict, Optional
from uuid import UUID
from fastapi import APIRouter, Depends, Request, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.notification import service

router = APIRouter()

class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

class NotificationResponse(CamelModel):
    id: UUID
    recipient_id: UUID
    sender_id: Optional[UUID] = None
    type: str
    title: str
    body: Optional[str] = None
    ref_type: Optional[str] = None
    ref_id: Optional[UUID] = None
    is_read: bool
    created_at: Any

from typing import Optional

# SSE event generator using in-memory queues
async def event_generator(request: Request, user_id: UUID):
    queue = service.manager.subscribe(user_id)
    try:
        # Send initial connect check event
        yield f"event: connect\ndata: Connected!\n\n"
        
        while True:
            if await request.is_disconnected():
                break
                
            try:
                # wait for notification event with a timeout for ping/keep-alive
                msg = await asyncio.wait_for(queue.get(), timeout=30.0)
                event = msg["event"]
                data = msg["data"]
                yield f"event: {event}\ndata: {json.dumps(data)}\n\n"
            except asyncio.TimeoutError:
                # Keep connection alive
                yield ": ping\n\n"
    finally:
        service.manager.disconnect(user_id, queue)

@router.get("/subscribe/{user_id}")
async def subscribe_notifications(
    user_id: UUID,
    request: Request,
) -> Any:
    """
    SSE endpoint for notifications. Returns a real-time event stream.
    """
    return StreamingResponse(event_generator(request, user_id), media_type="text/event-stream")

@router.get("/user/{user_id}", response_model=List[NotificationResponse])
async def read_notifications(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve notifications for user.
    """
    notifications = await service.get_notifications(db, user_id=user_id)
    return notifications

@router.get("/user/{user_id}/unread-count")
async def read_unread_count(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve unread notification count.
    """
    count = await service.get_unread_count(db, user_id=user_id)
    return {"count": count}

@router.put("/{id}/read", status_code=status.HTTP_200_OK)
async def mark_notification_as_read(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Mark a notification as read.
    """
    await service.mark_as_read(db, id=id)
    return {"message": "marked as read"}
