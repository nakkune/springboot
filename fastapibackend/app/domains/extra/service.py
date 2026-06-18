from typing import List, Optional
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.domains.extra.models import ActivityLog, Notification, Automation, Dashboard
from app.domains.extra import schemas
from app.domains.user.models import User

async def get_activity_logs(db: AsyncSession, workspace_id: UUID) -> List[ActivityLog]:
    result = await db.execute(
        select(ActivityLog).filter(ActivityLog.workspace_id == workspace_id).order_by(ActivityLog.created_at.desc())
    )
    return result.scalars().all()

async def get_activity_logs_by_item(db: AsyncSession, item_id: UUID) -> List[ActivityLog]:
    result = await db.execute(
        select(ActivityLog, User.full_name, User.avatar_url)
        .join(User, ActivityLog.actor_id == User.id)
        .filter(ActivityLog.item_id == item_id)
        .order_by(ActivityLog.created_at.desc())
    )
    
    logs = []
    for log, full_name, avatar_url in result.all():
        log.actor_name = full_name
        log.actor_avatar = avatar_url
        logs.append(log)
    return logs

async def create_activity_log(db: AsyncSession, *, log_in: schemas.ActivityLogCreate) -> ActivityLog:
    db_obj = ActivityLog(**log_in.model_dump())
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def get_notifications(db: AsyncSession, user_id: UUID) -> List[Notification]:
    result = await db.execute(
        select(Notification).filter(Notification.recipient_id == user_id).order_by(Notification.created_at.desc())
    )
    return result.scalars().all()

async def update_notification(db: AsyncSession, *, notification_id: UUID, obj_in: schemas.NotificationUpdate) -> Optional[Notification]:
    db_obj = await db.get(Notification, notification_id)
    if db_obj:
        update_data = obj_in.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_obj, field, value)
        db.add(db_obj)
        await db.commit()
        await db.refresh(db_obj)
    return db_obj

async def get_automations(db: AsyncSession, board_id: UUID) -> List[Automation]:
    result = await db.execute(select(Automation).filter(Automation.board_id == board_id))
    return result.scalars().all()

async def create_automation(db: AsyncSession, *, automation_in: schemas.AutomationCreate, user_id: UUID) -> Automation:
    db_obj = Automation(**automation_in.model_dump(), created_by=user_id)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def get_dashboards(db: AsyncSession, workspace_id: UUID) -> List[Dashboard]:
    result = await db.execute(select(Dashboard).filter(Dashboard.workspace_id == workspace_id))
    return result.scalars().all()

async def create_dashboard(db: AsyncSession, *, dashboard_in: schemas.DashboardCreate, user_id: UUID) -> Dashboard:
    db_obj = Dashboard(**dashboard_in.model_dump(), created_by=user_id)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj
