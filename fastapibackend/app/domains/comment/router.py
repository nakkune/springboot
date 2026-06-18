from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.comment import schemas, service

router = APIRouter()

@router.get("/items/{item_id}/comments", response_model=List[schemas.Comment])
async def read_comments(
    item_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_comments_by_item(db, item_id=item_id)

@router.post("/comments", response_model=schemas.Comment)
async def create_comment(
    comment_in: schemas.CommentCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_comment(db, comment_in=comment_in, author_id=current_user.id)

@router.put("/comments/{comment_id}", response_model=schemas.Comment)
async def update_comment(
    comment_id: UUID,
    obj_in: schemas.CommentUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    comment = await service.update_comment(db, comment_id=comment_id, obj_in=obj_in)
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")
    return comment

@router.delete("/comments/{comment_id}")
async def delete_comment(
    comment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    comment = await service.delete_comment(db, comment_id=comment_id)
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")
    return {"ok": True}
