from typing import Optional, List, Any
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

class WorkspaceBase(CamelModel):
    name: str
    slug: str
    logo_url: Optional[str] = None
    plan: Optional[str] = "free"

class WorkspaceCreate(WorkspaceBase):
    pass

class WorkspaceUpdate(CamelModel):
    name: Optional[str] = None
    slug: Optional[str] = None
    logo_url: Optional[str] = None
    plan: Optional[str] = None

class WorkspaceInDBBase(WorkspaceBase):
    id: UUID
    owner_id: UUID
    created_at: datetime
    updated_at: datetime

class Workspace(WorkspaceInDBBase):
    pass

class WorkspaceMemberBase(CamelModel):
    role: Optional[str] = "member"

class WorkspaceMemberCreate(WorkspaceMemberBase):
    user_id: UUID

class WorkspaceMemberUpdate(CamelModel):
    role: Optional[str] = None

class WorkspaceMemberInDBBase(WorkspaceMemberBase):
    id: UUID
    workspace_id: UUID
    user_id: UUID
    invited_by: Optional[UUID] = None
    joined_at: datetime

class WorkspaceMember(WorkspaceMemberInDBBase):
    email: Optional[str] = None
    full_name: Optional[str] = None
    avatar_url: Optional[str] = None
