from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.item import schemas, service

router = APIRouter()

@router.get("/board/{board_id}", response_model=List[schemas.Item])
async def read_items_by_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve items for a board.
    """
    items = await service.get_items_by_board(db, board_id=board_id)
    return items

@router.get("/group/{group_id}", response_model=List[schemas.Item])
async def read_items_by_group(
    group_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve items for a group.
    """
    items = await service.get_items_by_group(db, group_id=group_id)
    return items

@router.get("/{item_id}", response_model=schemas.Item)
async def read_item(
    item_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve item by ID.
    """
    item = await service.get_item(db, item_id=item_id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")
    return item

@router.post("", response_model=schemas.Item)
@router.post("/", response_model=schemas.Item)
async def create_item(
    *,
    db: AsyncSession = Depends(get_db),
    item_in: schemas.ItemCreate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Create a new item.
    """
    item = await service.create_item(db, item_in=item_in, user_id=current_user.id)
    return item

@router.put("/{item_id}", response_model=schemas.Item)
async def update_item(
    item_id: UUID,
    item_in: schemas.ItemUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Update an item (e.g. position, group, values).
    """
    item = await service.update_item(db, item_id=item_id, item_in=item_in, user_id=current_user.id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")
    return item

@router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_item(
    item_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Delete an item and its cascading dependencies.
    """
    success = await service.delete_item(db, item_id=item_id)
    if not success:
        raise HTTPException(status_code=404, detail="Item not found")
    return None


item_values_router = APIRouter()

@item_values_router.get("/item/{item_id}", response_model=List[schemas.ItemValue])
async def read_item_values_by_item(
    item_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_item_values_by_item(db, item_id=item_id)

@item_values_router.get("/board/{board_id}", response_model=List[schemas.ItemValue])
async def read_item_values_by_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_item_values_by_board(db, board_id=board_id)

@item_values_router.get("/item/{item_id}/column/{column_id}", response_model=schemas.ItemValue)
async def read_item_value_by_item_and_column(
    item_id: UUID,
    column_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    val = await service.get_item_value_by_item_and_column(db, item_id=item_id, column_id=column_id)
    if not val:
        raise HTTPException(status_code=404, detail="Item value not found")
    return val

@item_values_router.post("", response_model=schemas.ItemValue)
@item_values_router.post("/", response_model=schemas.ItemValue)
async def save_item_value(
    item_value_in: schemas.ItemValueCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_item_value(db, item_value_in=item_value_in, user_id=current_user.id)

@item_values_router.post("/bulk", response_model=List[schemas.ItemValue])
async def save_item_values_bulk(
    item_values_in: List[schemas.ItemValueCreate],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_item_values_bulk(db, item_values_in=item_values_in, user_id=current_user.id)

@item_values_router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_item_value(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    success = await service.delete_item_value(db, id=id)
    if not success:
        raise HTTPException(status_code=404, detail="Item value not found")
    return None
