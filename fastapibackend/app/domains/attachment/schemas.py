from typing import Optional
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

class AttachmentBase(CamelModel):
    item_id: Optional[UUID] = None
    comment_id: Optional[UUID] = None
    uploader_id: UUID
    file_name: str
    file_size: int
    mime_type: Optional[str] = None
    storage_url: str

class AttachmentCreate(AttachmentBase):
    pass

class AttachmentUpdate(CamelModel):
    pass

class Attachment(AttachmentBase):
    id: UUID
    created_at: datetime
    uploader_name: Optional[str] = None
