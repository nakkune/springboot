from typing import Optional, List, Dict, Any
from uuid import UUID
from datetime import datetime
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

class ActivityLogBase(CamelModel):
    workspace_id: Optional[UUID] = None
    project_id: Optional[UUID] = None
    board_id: Optional[UUID] = None
    item_id: Optional[UUID] = None
    action: str
    meta: Optional[Dict[str, Any]] = {}

class ActivityLogCreate(ActivityLogBase):
    actor_id: UUID

class ActivityLogInDBBase(ActivityLogBase):
    id: UUID
    actor_id: UUID
    created_at: datetime

class ActivityLog(ActivityLogInDBBase):
    actor_name: Optional[str] = None
    actor_avatar: Optional[str] = None

class NotificationBase(BaseModel):
    type: str
    title: str
    body: Optional[str] = None
    ref_type: Optional[str] = None
    ref_id: Optional[UUID] = None
    is_read: Optional[bool] = False

class NotificationCreate(NotificationBase):
    recipient_id: UUID
    sender_id: Optional[UUID] = None

class NotificationUpdate(BaseModel):
    is_read: Optional[bool] = None

class NotificationInDBBase(NotificationBase):
    id: UUID
    recipient_id: UUID
    sender_id: Optional[UUID] = None
    created_at: datetime

    class Config:
        from_attributes = True

class Notification(NotificationInDBBase):
    pass

class AutomationBase(BaseModel):
    name: str
    is_active: Optional[bool] = True
    trigger_config: Dict[str, Any]
    condition_config: Optional[Dict[str, Any]] = {}
    action_config: Dict[str, Any]

class AutomationCreate(AutomationBase):
    board_id: UUID

class AutomationUpdate(BaseModel):
    name: Optional[str] = None
    is_active: Optional[bool] = None
    trigger_config: Optional[Dict[str, Any]] = None
    condition_config: Optional[Dict[str, Any]] = None
    action_config: Optional[Dict[str, Any]] = None

class AutomationInDBBase(AutomationBase):
    id: UUID
    board_id: UUID
    run_count: int
    last_run_at: Optional[datetime] = None
    created_by: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

class Automation(AutomationInDBBase):
    pass

class DashboardBase(BaseModel):
    name: str
    is_shared: Optional[bool] = False

class DashboardCreate(DashboardBase):
    workspace_id: UUID

class DashboardUpdate(BaseModel):
    name: Optional[str] = None
    is_shared: Optional[bool] = None

class DashboardInDBBase(DashboardBase):
    id: UUID
    workspace_id: UUID
    share_token: Optional[str] = None
    created_by: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

class Dashboard(DashboardInDBBase):
    pass
