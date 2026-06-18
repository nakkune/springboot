from typing import Optional, List
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

class TeamBase(CamelModel):
    workspace_id: UUID
    name: str
    description: Optional[str] = None
    color: Optional[str] = None

class TeamCreate(TeamBase):
    pass

class TeamUpdate(CamelModel):
    name: Optional[str] = None
    description: Optional[str] = None
    color: Optional[str] = None

class TeamInDBBase(TeamBase):
    id: UUID
    created_by: UUID
    created_at: datetime
    updated_at: datetime

class Team(TeamInDBBase):
    pass

class TeamMemberBase(CamelModel):
    team_id: UUID
    user_id: UUID
    role: Optional[str] = "member"

class TeamMemberCreate(TeamMemberBase):
    pass

class TeamMemberUpdate(CamelModel):
    role: Optional[str] = None

class TeamMember(TeamMemberBase):
    id: UUID
    joined_at: datetime
