from typing import List, Optional
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.domains.comment.models import Comment
from app.domains.comment import schemas

async def get_comments_by_item(db: AsyncSession, item_id: UUID) -> List[Comment]:
    result = await db.execute(select(Comment).filter(Comment.item_id == item_id).order_by(Comment.created_at.asc()))
    return result.scalars().all()

async def create_comment(db: AsyncSession, *, comment_in: schemas.CommentCreate, author_id: UUID) -> Comment:
    db_obj = Comment(
        item_id=comment_in.item_id,
        parent_id=comment_in.parent_id,
        body=comment_in.body,
        author_id=author_id
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_comment(db: AsyncSession, *, comment_id: UUID, obj_in: schemas.CommentUpdate) -> Optional[Comment]:
    db_obj = await db.get(Comment, comment_id)
    if db_obj:
        db_obj.body = obj_in.body
        db_obj.is_edited = True
        db.add(db_obj)
        await db.commit()
        await db.refresh(db_obj)
    return db_obj

async def delete_comment(db: AsyncSession, *, comment_id: UUID) -> Optional[Comment]:
    db_obj = await db.get(Comment, comment_id)
    if db_obj:
        await db.delete(db_obj)
        await db.commit()
    return db_obj
