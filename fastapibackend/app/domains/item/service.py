from typing import List, Optional, Dict
from uuid import UUID, uuid4
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import update, delete, func
from sqlalchemy.orm import selectinload

from app.domains.item.models import Item, ItemValue
from app.domains.item.schemas import ItemCreate, ItemUpdate, ItemValueCreate
from app.domains.user.models import User
from app.domains.extra.models import ActivityLog
from app.domains.board.models import Board
from app.domains.project.models import Project

# Helper to populate values map for items
async def populate_items_values(db: AsyncSession, items: List[Item]) -> List[Item]:
    if not items:
        return items
    item_ids = [item.id for item in items]
    result = await db.execute(
        select(ItemValue).filter(ItemValue.item_id.in_(item_ids))
    )
    values = result.scalars().all()
    
    # Map item_id -> column_id -> value_text
    values_map = {}
    for val in values:
        if val.item_id not in values_map:
            values_map[val.item_id] = {}
        values_map[val.item_id][str(val.column_id)] = val.value_text or ""
        
    for item in items:
        item.values = values_map.get(item.id, {})
    return items

async def get_items_by_board(db: AsyncSession, board_id: UUID) -> List[Item]:
    result = await db.execute(
        select(Item)
        .filter(Item.board_id == board_id, Item.is_archived == False)
        .order_by(Item.position)
    )
    items = list(result.scalars().all())
    return await populate_items_values(db, items)

async def get_items_by_group(db: AsyncSession, group_id: UUID) -> List[Item]:
    result = await db.execute(
        select(Item)
        .filter(Item.group_id == group_id, Item.is_archived == False)
        .order_by(Item.position)
    )
    items = list(result.scalars().all())
    return await populate_items_values(db, items)

async def get_item(db: AsyncSession, item_id: UUID) -> Optional[Item]:
    result = await db.execute(select(Item).filter(Item.id == item_id))
    item = result.scalars().first()
    if item:
        items = await populate_items_values(db, [item])
        return items[0]
    return None

async def create_item(db: AsyncSession, item_in: ItemCreate, user_id: UUID) -> Item:
    # 10년 차 시니어 설계: 새 아이템 위치 산출 (그룹 내 맨 뒤)
    result = await db.execute(
        select(func.coalesce(func.max(Item.position), -1))
        .filter(Item.group_id == item_in.group_id)
    )
    max_pos = result.scalar()
    position = max_pos + 1

    db_obj = Item(
        id=uuid4(),
        name=item_in.name,
        board_id=item_in.board_id,
        group_id=item_in.group_id,
        parent_item_id=item_in.parent_item_id,
        position=position,
        is_archived=False,
        created_by=user_id,
    )
    db.add(db_obj)
    await db.flush()
    
    # Save values if any
    if item_in.values:
        for col_id_str, val_text in item_in.values.items():
            db_val = ItemValue(
                id=uuid4(),
                item_id=db_obj.id,
                column_id=UUID(col_id_str),
                value_text=val_text,
                updated_by=user_id
            )
            db.add(db_val)
            
    await db.commit()
    return await get_item(db, db_obj.id)

async def update_item(db: AsyncSession, item_id: UUID, item_in: ItemUpdate, user_id: UUID) -> Optional[Item]:
    db_obj = await get_item(db, item_id)
    if not db_obj:
        return None
        
    old_group_id = db_obj.group_id
    old_position = db_obj.position
    
    group_changed = item_in.group_id is not None and item_in.group_id != old_group_id
    position_changed = item_in.position is not None and item_in.position != old_position
    
    if item_in.name is not None:
        db_obj.name = item_in.name
    if item_in.is_archived is not None:
        db_obj.is_archived = item_in.is_archived
        
    # 10년 차 시니어 드래그앤드롭 포지셔닝 무결성 보장(Position Reordering) 알고리즘
    if group_changed or position_changed:
        new_group_id = item_in.group_id if group_changed else old_group_id
        new_position = item_in.position if position_changed else old_position
        
        # 1. 기존 그룹에서 당겨와서 메꿈
        if group_changed:
            await db.execute(
                update(Item)
                .where(Item.group_id == old_group_id, Item.position > old_position)
                .values(position=Item.position - 1)
            )
            
        # 2. 신규 그룹에서 밀어 밀어 자리 확보
        await db.execute(
            update(Item)
            .where(Item.group_id == new_group_id, Item.id != item_id, Item.position >= new_position)
            .values(position=Item.position + 1)
        )
        
        db_obj.group_id = new_group_id
        db_obj.position = new_position
        
    db.add(db_obj)
    
    # Update EAV values if any
    if item_in.values is not None:
        for col_id_str, val_text in item_in.values.items():
            col_uuid = UUID(col_id_str)
            val_res = await db.execute(
                select(ItemValue).filter(ItemValue.item_id == item_id, ItemValue.column_id == col_uuid)
            )
            existing_val = val_res.scalars().first()
            if existing_val:
                existing_val.value_text = val_text
                existing_val.updated_by = user_id
                db.add(existing_val)
            else:
                new_val = ItemValue(
                    id=uuid4(),
                    item_id=item_id,
                    column_id=col_uuid,
                    value_text=val_text,
                    updated_by=user_id
                )
                db.add(new_val)
                
    await db.commit()
    return await get_item(db, item_id)

async def delete_item(db: AsyncSession, item_id: UUID) -> bool:
    db_obj = await get_item(db, item_id)
    if not db_obj:
        return False
        
    # 1. Recursive cascade delete children
    children_res = await db.execute(
        select(Item).filter(Item.parent_item_id == item_id)
    )
    children = children_res.scalars().all()
    for child in children:
        await delete_item(db, child.id)
        
    # 2. Delete EAV values
    await db.execute(
        delete(ItemValue).where(ItemValue.item_id == item_id)
    )
    
    # 3. Delete item itself
    await db.delete(db_obj)
    await db.commit()
    return True


async def get_item_values_by_item(db: AsyncSession, item_id: UUID) -> List[ItemValue]:
    result = await db.execute(
        select(ItemValue).filter(ItemValue.item_id == item_id).order_by(ItemValue.updated_at.desc())
    )
    return list(result.scalars().all())


async def get_item_values_by_board(db: AsyncSession, board_id: UUID) -> List[ItemValue]:
    result = await db.execute(
        select(ItemValue)
        .join(Item, ItemValue.item_id == Item.id)
        .filter(Item.board_id == board_id)
        .order_by(ItemValue.updated_at.desc())
    )
    return list(result.scalars().all())


async def get_item_value_by_item_and_column(db: AsyncSession, item_id: UUID, column_id: UUID) -> Optional[ItemValue]:
    result = await db.execute(
        select(ItemValue).filter(ItemValue.item_id == item_id, ItemValue.column_id == column_id)
    )
    return result.scalars().first()


async def save_item_value(db: AsyncSession, item_value_in: ItemValueCreate, user_id: UUID, commit: bool = True) -> ItemValue:
    result = await db.execute(
        select(ItemValue).filter(
            ItemValue.item_id == item_value_in.item_id,
            ItemValue.column_id == item_value_in.column_id
        )
    )
    db_obj = result.scalars().first()
    
    updated_by = item_value_in.updated_by or user_id
    
    import json
    val_json = item_value_in.value_json
    if isinstance(val_json, str):
        try:
            val_json = json.loads(val_json)
        except Exception:
            pass
            
    if db_obj:
        if item_value_in.value_text is not None:
            db_obj.value_text = item_value_in.value_text
        if item_value_in.value_number is not None:
            db_obj.value_number = item_value_in.value_number
        if item_value_in.value_date is not None:
            db_obj.value_date = item_value_in.value_date
        if val_json is not None:
            db_obj.value_json = val_json
        db_obj.updated_by = updated_by
    else:
        db_obj = ItemValue(
            id=item_value_in.id or uuid4(),
            item_id=item_value_in.item_id,
            column_id=item_value_in.column_id,
            value_text=item_value_in.value_text,
            value_number=item_value_in.value_number,
            value_date=item_value_in.value_date,
            value_json=val_json,
            updated_by=updated_by
        )
        db.add(db_obj)
        
    await db.flush()
    
    # 10년 차 시니어 설계: 활동 로그 기록 연계
    try:
        item_res = await db.execute(
            select(Item.board_id, Board.project_id, Project.workspace_id)
            .join(Board, Item.board_id == Board.id)
            .join(Project, Board.project_id == Project.id)
            .filter(Item.id == item_value_in.item_id)
        )
        item_ctx = item_res.first()
        if item_ctx:
            board_id, project_id, workspace_id = item_ctx
            meta_dict = {"columnId": str(item_value_in.column_id)}
            if item_value_in.value_text is not None:
                meta_dict["value"] = item_value_in.value_text
            elif val_json is not None:
                meta_dict["value"] = val_json
                
            activity_log = ActivityLog(
                id=uuid4(),
                workspace_id=workspace_id,
                project_id=project_id,
                board_id=board_id,
                item_id=item_value_in.item_id,
                actor_id=updated_by,
                action="item.update",
                meta=meta_dict
            )
            db.add(activity_log)
    except Exception as e:
        # Ignore logging errors to prevent breaking database transaction
        pass

    if commit:
        await db.commit()
        db_obj = await get_item_value_by_item_and_column(db, item_value_in.item_id, item_value_in.column_id)
        
    return db_obj


async def save_item_values_bulk(db: AsyncSession, item_values_in: List[ItemValueCreate], user_id: UUID) -> List[ItemValue]:
    results = []
    for iv in item_values_in:
        results.append(await save_item_value(db, iv, user_id, commit=False))
    await db.commit()
    # Return populated models
    reloaded_results = []
    for iv in item_values_in:
        db_obj = await get_item_value_by_item_and_column(db, iv.item_id, iv.column_id)
        if db_obj:
            reloaded_results.append(db_obj)
    return reloaded_results


async def delete_item_value(db: AsyncSession, id: UUID) -> bool:
    result = await db.execute(select(ItemValue).filter(ItemValue.id == id))
    db_obj = result.scalars().first()
    if db_obj:
        await db.delete(db_obj)
        await db.commit()
        return True
    return False
