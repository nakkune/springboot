from typing import Optional, List, Dict, Any
from uuid import UUID
from datetime import datetime, date
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

class ItemValueBase(CamelModel):
    value_text: Optional[str] = None
    value_number: Optional[float] = None
    value_date: Optional[date] = None
    value_json: Optional[Any] = None

class ItemValueCreate(ItemValueBase):
    id: Optional[UUID] = None
    item_id: UUID
    column_id: UUID
    updated_by: Optional[UUID] = None

class ItemValue(ItemValueBase):
    id: UUID
    item_id: UUID
    column_id: UUID
    updated_by: Optional[UUID] = None
    updated_at: Optional[datetime] = None

class ItemBase(CamelModel):
    name: str

class ItemCreate(ItemBase):
    board_id: UUID
    group_id: UUID
    parent_item_id: Optional[UUID] = None
    values: Optional[Dict[str, str]] = {}

class ItemUpdate(CamelModel):
    name: Optional[str] = None
    group_id: Optional[UUID] = None
    position: Optional[int] = None
    is_archived: Optional[bool] = None
    values: Optional[Dict[str, str]] = None

class Item(ItemBase):
    id: UUID
    board_id: UUID
    group_id: UUID
    parent_item_id: Optional[UUID] = None
    position: int
    is_archived: bool
    created_by: UUID
    created_at: datetime
    updated_at: datetime
    values: Dict[str, str] = {}
