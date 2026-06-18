from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.extra import schemas, service

router = APIRouter()

@router.get("/workspaces/{workspace_id}/activity-logs", response_model=List[schemas.ActivityLog])
async def read_activity_logs(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_activity_logs(db, workspace_id=workspace_id)

@router.get("/activity-logs/item/{itemId}", response_model=List[schemas.ActivityLog])
async def get_activity_logs_by_item_id(
    itemId: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    특정 아이템 ID에 해당하는 모든 활동 로그 목록을 조회합니다.
    """
    return await service.get_activity_logs_by_item(db, item_id=itemId)

@router.get("/notifications", response_model=List[schemas.Notification])
async def read_notifications(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_notifications(db, user_id=current_user.id)

@router.put("/notifications/{notification_id}", response_model=schemas.Notification)
async def update_notification(
    notification_id: UUID,
    obj_in: schemas.NotificationUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    notification = await service.update_notification(db, notification_id=notification_id, obj_in=obj_in)
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    return notification

@router.get("/boards/{board_id}/automations", response_model=List[schemas.Automation])
async def read_automations(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_automations(db, board_id=board_id)

@router.post("/automations", response_model=schemas.Automation)
async def create_automation(
    automation_in: schemas.AutomationCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_automation(db, automation_in=automation_in, user_id=current_user.id)

@router.get("/workspaces/{workspace_id}/dashboards", response_model=List[schemas.Dashboard])
async def read_dashboards(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_dashboards(db, workspace_id=workspace_id)

@router.post("/dashboards", response_model=schemas.Dashboard)
async def create_dashboard(
    dashboard_in: schemas.DashboardCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_dashboard(db, dashboard_in=dashboard_in, user_id=current_user.id)
