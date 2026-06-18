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

class BoardBase(CamelModel):
    name: str
    description: Optional[str] = None
    board_type: Optional[str] = "main"

class BoardCreate(BoardBase):
    project_id: UUID

class BoardUpdate(CamelModel):
    name: Optional[str] = None
    description: Optional[str] = None
    board_type: Optional[str] = None
    position: Optional[int] = None
    is_archived: Optional[bool] = None

class Board(BoardBase):
    id: UUID
    project_id: UUID
    position: int
    is_archived: bool
    created_by: UUID
    created_at: datetime
    updated_at: datetime


class BoardColumnBase(CamelModel):
    name: str
    column_type: str
    settings: Optional[Dict[str, Any]] = {}
    is_required: Optional[bool] = False
    is_hidden: Optional[bool] = False

class BoardColumnCreate(BoardColumnBase):
    board_id: UUID

class BoardColumnUpdate(CamelModel):
    name: Optional[str] = None
    column_type: Optional[str] = None
    settings: Optional[Dict[str, Any]] = None
    is_required: Optional[bool] = None
    is_hidden: Optional[bool] = None
    position: Optional[int] = None

class BoardColumn(BoardColumnBase):
    id: UUID
    board_id: UUID
    position: int
    created_at: datetime
    updated_at: datetime


class BoardGroupBase(CamelModel):
    title: str
    color: Optional[str] = None

class BoardGroupCreate(BoardGroupBase):
    board_id: UUID

class BoardGroupUpdate(CamelModel):
    title: Optional[str] = None
    color: Optional[str] = None
    position: Optional[int] = None
    is_collapsed: Optional[bool] = None

class BoardGroup(BoardGroupBase):
    id: UUID
    board_id: UUID
    position: int
    is_collapsed: bool
    created_at: datetime
    updated_at: datetime
