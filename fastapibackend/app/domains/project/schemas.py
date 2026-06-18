from typing import Optional
from uuid import UUID
from pydantic import BaseModel

class ProjectBase(BaseModel):
    name: str
    description: Optional[str] = None
    color: Optional[str] = None
    icon: Optional[str] = None

class ProjectCreate(ProjectBase):
    workspace_id: UUID
    team_id: Optional[UUID] = None

class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    color: Optional[str] = None
    icon: Optional[str] = None
    is_archived: Optional[bool] = None

class ProjectInDBBase(ProjectBase):
    id: UUID
    workspace_id: UUID
    team_id: Optional[UUID] = None
    is_archived: bool
    created_by: UUID

    class Config:
        from_attributes = True

class Project(ProjectInDBBase):
    pass
