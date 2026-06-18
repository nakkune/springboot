import pytest
import pytest_asyncio
from uuid import uuid4
from sqlalchemy.future import select

from app.domains.user.models import User
from app.domains.workspace.models import Workspace
from app.domains.project.models import Project
from app.domains.board.models import Board, BoardGroup, BoardColumn
from app.domains.item.models import Item, ItemValue
from app.domains.item.schemas import ItemCreate, ItemUpdate
from app.domains.item.service import create_item, update_item, delete_item, get_items_by_group
from app.db.session import AsyncSessionLocal

@pytest_asyncio.fixture
async def db_session():
    async with AsyncSessionLocal() as session:
        yield session

@pytest.mark.asyncio
async def test_item_position_and_eav(db_session):
    # 1. Setup setup workspace, project, board, columns, groups
    user_id = uuid4()
    ws_id = uuid4()
    proj_id = uuid4()
    board_id = uuid4()
    group1_id = uuid4()
    group2_id = uuid4()
    col_id = uuid4()

    # User
    user = User(
        id=user_id,
        email=f"test_item_{uuid4().hex[:6]}@example.com",
        password_hash="test",
        full_name="홍길동",
        role="member",
        member_status="active",
        is_active=True
    )
    db_session.add(user)
    await db_session.flush()

    # Workspace
    ws = Workspace(id=ws_id, name="Test WS", slug=f"test-ws-{uuid4().hex[:6]}", owner_id=user_id)
    db_session.add(ws)
    await db_session.flush()

    # Project
    proj = Project(id=proj_id, workspace_id=ws_id, name="Test Project", created_by=user_id)
    db_session.add(proj)
    await db_session.flush()

    # Board
    board = Board(id=board_id, project_id=proj_id, name="Test Board", created_by=user_id)
    db_session.add(board)
    await db_session.flush()

    # Columns & Groups
    g1 = BoardGroup(id=group1_id, board_id=board_id, title="Group 1", position=0)
    g2 = BoardGroup(id=group2_id, board_id=board_id, title="Group 2", position=1)
    db_session.add(g1)
    db_session.add(g2)

    col = BoardColumn(id=col_id, board_id=board_id, name="Status", column_type="status", position=0)
    db_session.add(col)

    await db_session.commit()

    # 2. Create items in Group 1
    # Items should be ordered 0, 1, 2
    item1_in = ItemCreate(name="Task 1", board_id=board_id, group_id=group1_id, values={str(col_id): "working"})
    item2_in = ItemCreate(name="Task 2", board_id=board_id, group_id=group1_id, values={str(col_id): "stuck"})
    item3_in = ItemCreate(name="Task 3", board_id=board_id, group_id=group1_id, values={str(col_id): "done"})

    item1 = await create_item(db_session, item1_in, user_id)
    item2 = await create_item(db_session, item2_in, user_id)
    item3 = await create_item(db_session, item3_in, user_id)

    assert item1.position == 0
    assert item2.position == 1
    assert item3.position == 2
    assert item1.values.get(str(col_id)) == "working"

    # 3. Test drag & drop order reordering within same group
    # Move item3 (pos 2) to pos 0.
    # Expected: item3 pos 0, item1 pos 1, item2 pos 2
    updated_item3 = await update_item(db_session, item3.id, ItemUpdate(position=0), user_id)
    
    # Query all items in group 1 to verify order
    items_g1 = await get_items_by_group(db_session, group1_id)
    assert len(items_g1) == 3
    
    # Verify items are in order: item3, item1, item2
    assert items_g1[0].id == item3.id
    assert items_g1[0].position == 0
    assert items_g1[1].id == item1.id
    assert items_g1[1].position == 1
    assert items_g1[2].id == item2.id
    assert items_g1[2].position == 2

    # 4. Test drag & drop move to different group
    # Move item1 (which is at pos 1 in group 1) to pos 0 in group 2.
    # Group 1 items left: item3 (pos 0), item2 (pos 2 -> should become pos 1)
    # Group 2 items: item1 (pos 0)
    updated_item1 = await update_item(db_session, item1.id, ItemUpdate(group_id=group2_id, position=0), user_id)

    items_g1_after = await get_items_by_group(db_session, group1_id)
    assert len(items_g1_after) == 2
    assert items_g1_after[0].id == item3.id
    assert items_g1_after[0].position == 0
    assert items_g1_after[1].id == item2.id
    assert items_g1_after[1].position == 1  # Correctly shifted down/up to fill gap!

    items_g2 = await get_items_by_group(db_session, group2_id)
    assert len(items_g2) == 1
    assert items_g2[0].id == item1.id
    assert items_g2[0].position == 0

    # 5. Clean up
    await delete_item(db_session, item1.id)
    await delete_item(db_session, item2.id)
    await delete_item(db_session, item3.id)
    await db_session.delete(col)
    await db_session.delete(g1)
    await db_session.delete(g2)
    await db_session.delete(board)
    await db_session.delete(proj)
    await db_session.delete(ws)
    await db_session.flush()
    await db_session.delete(user)
    await db_session.commit()
