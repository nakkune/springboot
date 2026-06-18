from typing import List, Optional
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.domains.board.models import Board, BoardColumn, BoardGroup
from app.domains.board.schemas import BoardCreate, BoardUpdate

async def get_boards_by_project(db: AsyncSession, project_id: UUID) -> List[Board]:
    result = await db.execute(
        select(Board)
        .filter(Board.project_id == project_id, Board.is_archived == False)
        .order_by(Board.position)
    )
    return result.scalars().all()

async def get_board(db: AsyncSession, board_id: UUID) -> Optional[Board]:
    result = await db.execute(select(Board).filter(Board.id == board_id))
    return result.scalars().first()

async def create_board(db: AsyncSession, board_in: BoardCreate, user_id: UUID) -> Board:
    db_obj = Board(
        name=board_in.name,
        description=board_in.description,
        board_type=board_in.board_type,
        project_id=board_in.project_id,
        created_by=user_id,
        position=0,
        is_archived=False
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    
    # 10년 차 시니어 설계: 새 보드 생성 시 즉각 업무를 시작할 수 있도록 필수 4대 핵심 컬럼 기본 셋팅 (상태, 담당자, 마감일, 타임라인, 우선순위)
    status_settings = {
        "options": [
            {"id": "1", "label": "완료", "color": "#00C875"},
            {"id": "2", "label": "진행 중", "color": "#FDAB3D"},
            {"id": "3", "label": "시작 전", "color": "#C4C4C4"},
            {"id": "4", "label": "막힘", "color": "#E2445C"}
        ]
    }
    
    col_status = BoardColumn(board_id=db_obj.id, name="상태", column_type="status", settings=status_settings, position=1)
    col_person = BoardColumn(board_id=db_obj.id, name="담당자", column_type="person", settings={}, position=2)
    col_date = BoardColumn(board_id=db_obj.id, name="마감일", column_type="date", settings={}, position=3)
    col_timeline = BoardColumn(board_id=db_obj.id, name="타임라인", column_type="timeline", settings={}, position=4)
    col_priority = BoardColumn(board_id=db_obj.id, name="우선순위", column_type="priority", settings={}, position=5)
    
    group_work = BoardGroup(board_id=db_obj.id, title="작업", color="#579BFC", position=1, is_collapsed=False)
    
    db.add(col_status)
    db.add(col_person)
    db.add(col_date)
    db.add(col_timeline)
    db.add(col_priority)
    db.add(group_work)
    
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_board(db: AsyncSession, board_id: UUID, board_in: BoardUpdate) -> Optional[Board]:
    db_obj = await get_board(db, board_id)
    if not db_obj:
        return None
    update_data = board_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_board(db: AsyncSession, board_id: UUID) -> bool:
    db_obj = await get_board(db, board_id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True

async def get_board_columns(db: AsyncSession, board_id: UUID) -> List[BoardColumn]:
    result = await db.execute(
        select(BoardColumn)
        .filter(BoardColumn.board_id == board_id)
        .order_by(BoardColumn.position)
    )
    return result.scalars().all()

async def get_board_groups(db: AsyncSession, board_id: UUID) -> List[BoardGroup]:
    result = await db.execute(
        select(BoardGroup)
        .filter(BoardGroup.board_id == board_id)
        .order_by(BoardGroup.position)
    )
    return result.scalars().all()


# --- BoardColumn 단건 CRUD ---
async def get_board_column(db: AsyncSession, id: UUID) -> Optional[BoardColumn]:
    result = await db.execute(select(BoardColumn).filter(BoardColumn.id == id))
    return result.scalars().first()

async def create_board_column(db: AsyncSession, column_in) -> BoardColumn:
    from sqlalchemy import func as sa_func
    # position: 해당 board의 마지막 +1
    max_pos_result = await db.execute(
        select(sa_func.coalesce(sa_func.max(BoardColumn.position), -1))
        .filter(BoardColumn.board_id == column_in.board_id)
    )
    max_pos = max_pos_result.scalar()
    db_obj = BoardColumn(
        board_id=column_in.board_id,
        name=column_in.name,
        column_type=column_in.column_type,
        settings=column_in.settings or {},
        position=max_pos + 1,
        is_required=column_in.is_required or False,
        is_hidden=column_in.is_hidden or False,
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_board_column(db: AsyncSession, id: UUID, column_in) -> Optional[BoardColumn]:
    db_obj = await get_board_column(db, id)
    if not db_obj:
        return None
    update_data = column_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        if hasattr(db_obj, field):
            setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_board_column(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_board_column(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# --- BoardGroup 단건 CRUD ---
async def get_board_group(db: AsyncSession, id: UUID) -> Optional[BoardGroup]:
    result = await db.execute(select(BoardGroup).filter(BoardGroup.id == id))
    return result.scalars().first()

async def create_board_group(db: AsyncSession, group_in) -> BoardGroup:
    from sqlalchemy import func as sa_func
    max_pos_result = await db.execute(
        select(sa_func.coalesce(sa_func.max(BoardGroup.position), -1))
        .filter(BoardGroup.board_id == group_in.board_id)
    )
    max_pos = max_pos_result.scalar()
    db_obj = BoardGroup(
        board_id=group_in.board_id,
        title=group_in.title,
        color=group_in.color or "#579BFC",
        position=max_pos + 1,
        is_collapsed=False,
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_board_group(db: AsyncSession, id: UUID, group_in) -> Optional[BoardGroup]:
    db_obj = await get_board_group(db, id)
    if not db_obj:
        return None
    update_data = group_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        if hasattr(db_obj, field):
            setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_board_group(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_board_group(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True

