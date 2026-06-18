from typing import Optional, List
from uuid import UUID
from datetime import datetime
from pydantic import BaseModel

class CommentBase(BaseModel):
    body: str

class CommentCreate(CommentBase):
    item_id: UUID
    parent_id: Optional[UUID] = None

class CommentUpdate(BaseModel):
    body: str

class CommentInDBBase(CommentBase):
    id: UUID
    item_id: UUID
    parent_id: Optional[UUID] = None
    author_id: UUID
    is_edited: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

class Comment(CommentInDBBase):
    pass

class CommentMentionBase(BaseModel):
    mentioned_user: UUID

class CommentMentionCreate(CommentMentionBase):
    comment_id: UUID

class CommentMentionInDBBase(CommentMentionBase):
    id: UUID
    comment_id: UUID

    class Config:
        from_attributes = True

class CommentMention(CommentMentionInDBBase):
    pass
