from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.board import schemas, service

router = APIRouter()

@router.get("/project/{project_id}", response_model=List[schemas.Board])
async def read_boards(
    project_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve boards for a project.
    """
    boards = await service.get_boards_by_project(db, project_id=project_id)
    return boards

@router.get("/{board_id}", response_model=schemas.Board)
async def read_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve a board by ID.
    """
    board = await service.get_board(db, board_id=board_id)
    if not board:
        raise HTTPException(status_code=404, detail="Board not found")
    return board

@router.post("", response_model=schemas.Board)
@router.post("/", response_model=schemas.Board)
async def create_board(
    *,
    db: AsyncSession = Depends(get_db),
    board_in: schemas.BoardCreate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Create a new board.
    """
    board = await service.create_board(db, board_in=board_in, user_id=current_user.id)
    return board

@router.put("/{board_id}", response_model=schemas.Board)
async def update_board(
    board_id: UUID,
    board_in: schemas.BoardUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Update a board.
    """
    board = await service.update_board(db, board_id=board_id, board_in=board_in)
    if not board:
        raise HTTPException(status_code=404, detail="Board not found")
    return board

@router.delete("/{board_id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Delete a board.
    """
    success = await service.delete_board(db, board_id=board_id)
    if not success:
        raise HTTPException(status_code=404, detail="Board not found")
    return None

@router.get("/{board_id}/columns", response_model=List[schemas.BoardColumn])
async def read_board_columns(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_board_columns(db, board_id=board_id)

@router.get("/{board_id}/groups", response_model=List[schemas.BoardGroup])
async def read_board_groups(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_board_groups(db, board_id=board_id)


# =============================================================
# ── Spring Boot 호환: /board-columns 독립 라우터               ──
# =============================================================
board_columns_router = APIRouter()

@board_columns_router.get("/board/{board_id}", response_model=List[schemas.BoardColumn])
async def read_columns_by_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_board_columns(db, board_id=board_id)

@board_columns_router.get("/{id}", response_model=schemas.BoardColumn)
async def read_column_by_id(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    col = await service.get_board_column(db, id)
    if not col:
        raise HTTPException(status_code=404, detail="Board column not found")
    return col

@board_columns_router.post("", response_model=schemas.BoardColumn, status_code=status.HTTP_201_CREATED)
async def create_column(
    column_in: schemas.BoardColumnCreate,
    board_id: UUID = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_board_column(db, column_in=column_in)

@board_columns_router.put("/{id}", response_model=schemas.BoardColumn)
async def update_column(
    id: UUID,
    column_in: schemas.BoardColumnUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    col = await service.update_board_column(db, id, column_in)
    if not col:
        raise HTTPException(status_code=404, detail="Board column not found")
    return col

@board_columns_router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_column(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_board_column(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Board column not found")
    return None


# =============================================================
# ── Spring Boot 호환: /board-groups 독립 라우터                 ──
# =============================================================
board_groups_router = APIRouter()

@board_groups_router.get("/board/{board_id}", response_model=List[schemas.BoardGroup])
async def read_groups_by_board(
    board_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_board_groups(db, board_id=board_id)

@board_groups_router.get("/{id}", response_model=schemas.BoardGroup)
async def read_group_by_id(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    grp = await service.get_board_group(db, id)
    if not grp:
        raise HTTPException(status_code=404, detail="Board group not found")
    return grp

@board_groups_router.post("", response_model=schemas.BoardGroup, status_code=status.HTTP_201_CREATED)
async def create_group(
    group_in: schemas.BoardGroupCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_board_group(db, group_in=group_in)

@board_groups_router.put("/{id}", response_model=schemas.BoardGroup)
async def update_group(
    id: UUID,
    group_in: schemas.BoardGroupUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    grp = await service.update_board_group(db, id, group_in)
    if not grp:
        raise HTTPException(status_code=404, detail="Board group not found")
    return grp

@board_groups_router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_group(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_board_group(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Board group not found")
    return None

