import pytest
import pytest_asyncio
import io
import os
from uuid import uuid4
from fastapi import UploadFile
from sqlalchemy.future import select

from app.domains.user.models import User
from app.domains.workspace.models import Workspace
from app.domains.project.models import Project
from app.domains.board.models import Board, BoardGroup
from app.domains.item.models import Item
from app.domains.team.models import Team
from app.domains.team.schemas import TeamCreate, TeamUpdate
from app.domains.team.service import create_team, get_teams_by_workspace, update_team, delete_team
from app.domains.attachment.models import Attachment
from app.domains.attachment.service import upload_attachment, get_attachments_by_item
from app.domains.extra.service import get_activity_logs_by_item
from app.db.session import AsyncSessionLocal

@pytest_asyncio.fixture
async def db_session():
    async with AsyncSessionLocal() as session:
        yield session

@pytest.mark.asyncio
async def test_team_crud_and_attachment_logs(db_session):
    # 1. Setup User, Workspace, Project, Board, Group, Item
    user_id = uuid4()
    ws_id = uuid4()
    proj_id = uuid4()
    board_id = uuid4()
    group_id = uuid4()
    item_id = uuid4()

    user = User(
        id=user_id,
        email=f"test_add_{uuid4().hex[:6]}@example.com",
        password_hash="test",
        full_name="이순신",
        role="member",
        member_status="active",
        is_active=True
    )
    db_session.add(user)
    await db_session.flush()

    ws = Workspace(id=ws_id, name="Test WS", slug=f"test-ws-{uuid4().hex[:6]}", owner_id=user_id)
    db_session.add(ws)
    await db_session.flush()

    proj = Project(id=proj_id, workspace_id=ws_id, name="Test Project", created_by=user_id)
    db_session.add(proj)
    await db_session.flush()

    board = Board(id=board_id, project_id=proj_id, name="Test Board", created_by=user_id)
    db_session.add(board)
    await db_session.flush()

    group = BoardGroup(id=group_id, board_id=board_id, title="Group 1", position=0)
    db_session.add(group)
    await db_session.flush()

    item = Item(
        id=item_id,
        name="Task with Attachment",
        board_id=board_id,
        group_id=group_id,
        position=0,
        is_archived=False,
        created_by=user_id
    )
    db_session.add(item)
    await db_session.commit()

    # 2. Team CRUD Verification
    team_in = TeamCreate(
        workspace_id=ws_id,
        name="Backend Team",
        description="Core Engineers",
        color="#FF0000"
    )
    db_team = await create_team(db_session, team_in=team_in, user_id=user_id)
    assert db_team.name == "Backend Team"
    assert db_team.workspace_id == ws_id
    assert db_team.created_by == user_id

    # List
    teams = await get_teams_by_workspace(db_session, workspace_id=ws_id)
    assert len(teams) == 1
    assert teams[0].id == db_team.id

    # Update
    team_up = TeamUpdate(name="Frontend Team", color="#0000FF")
    updated_team = await update_team(db_session, db_obj=db_team, obj_in=team_up)
    assert updated_team.name == "Frontend Team"
    assert updated_team.color == "#0000FF"

    # 3. Attachment Upload Verification
    from starlette.datastructures import Headers
    file_content = b"Mock document content for verification"
    mock_file = UploadFile(
        file=io.BytesIO(file_content),
        filename="report.pdf",
        headers=Headers({"content-type": "application/pdf"})
    )

    db_attachment = await upload_attachment(
        db_session,
        item_id=item_id,
        uploader_id=user_id,
        file=mock_file
    )
    assert db_attachment.file_name == "report.pdf"
    assert db_attachment.uploader_id == user_id
    assert db_attachment.item_id == item_id
    assert db_attachment.storage_url.startswith("/uploads/")

    # Get attachments
    attachments = await get_attachments_by_item(db_session, item_id=item_id)
    assert len(attachments) == 1
    assert attachments[0].id == db_attachment.id
    assert attachments[0].uploader_name == "이순신"

    # Verify physical file deletion later by recording the path
    saved_filename = db_attachment.storage_url.split("/")[-1]
    saved_filepath = os.path.join(os.getcwd(), "uploads", saved_filename)
    assert os.path.exists(saved_filepath)

    # 4. Activity Log Verification
    activity_logs = await get_activity_logs_by_item(db_session, item_id=item_id)
    assert len(activity_logs) >= 1
    # Check that actor details are joined
    assert activity_logs[0].actor_name == "이순신"
    assert activity_logs[0].action == "attachment.add"
    assert activity_logs[0].meta.get("fileName") == "report.pdf"

    # 5. Cleanup Database & Physical Files
    # Remove database entries
    await delete_team(db_session, id=db_team.id)
    
    # Delete attachment (db cascading delete should clean up activity_logs and attachments but let's do it cleanly)
    await db_session.delete(db_attachment)
    for log in activity_logs:
        # Resolve session state to avoid conflicts
        merged_log = await db_session.get(log.__class__, log.id)
        if merged_log:
            await db_session.delete(merged_log)

    await db_session.delete(item)
    await db_session.delete(group)
    await db_session.delete(board)
    await db_session.delete(proj)
    await db_session.delete(ws)
    await db_session.flush()
    await db_session.delete(user)
    await db_session.commit()

    # Delete physical file
    if os.path.exists(saved_filepath):
        os.remove(saved_filepath)


@pytest.mark.asyncio
async def test_workspace_member_and_settings(db_session):
    from app.domains.workspace.service import (
        get_workspace_members,
        invite_workspace_member,
        update_workspace_member_role,
        delete_workspace_member
    )
    from app.domains.user.service import update_user
    from app.domains.user.schemas import UserUpdate

    # Create test owner User
    owner_id = uuid4()
    owner = User(
        id=owner_id,
        email=f"owner_{uuid4().hex[:6]}@example.com",
        password_hash="test",
        full_name="Owner User",
        is_active=True
    )
    db_session.add(owner)
    
    ws_id = uuid4()
    ws = Workspace(id=ws_id, name="Settings Test WS", slug=f"settings-ws-{uuid4().hex[:6]}", owner_id=owner_id)
    db_session.add(ws)
    await db_session.commit()
    
    # 1. Invite a non-existing user
    invited_email = f"invited_{uuid4().hex[:6]}@example.com"
    member = await invite_workspace_member(
        db_session,
        workspace_id=ws_id,
        email=invited_email,
        role="member",
        invited_by=owner_id
    )
    assert member.email == invited_email
    assert member.role == "member"
    assert member.full_name is not None
    
    # 2. Verify members list
    members = await get_workspace_members(db_session, workspace_id=ws_id)
    assert len(members) >= 1
    assert any(m.email == invited_email for m in members)
    
    # 3. Update member role
    updated_member = await update_workspace_member_role(
        db_session,
        member_id=member.id,
        role="admin"
    )
    assert updated_member.role == "admin"
    
    # 4. Update owner preferences
    user_up = UserUpdate(theme="light", timezone="Europe/London")
    updated_owner = await update_user(db_session, db_obj=owner, obj_in=user_up)
    assert updated_owner.theme == "light"
    assert updated_owner.timezone == "Europe/London"
    
    # 5. Cleanup (order matters due to FK constraints)
    # Delete workspace member first
    await delete_workspace_member(db_session, member_id=member.id)
    
    # Delete invited user (no FK dependencies)
    invited_user_res = await db_session.execute(select(User).filter(User.email == invited_email))
    invited_user = invited_user_res.scalars().first()
    if invited_user:
        await db_session.delete(invited_user)
        await db_session.flush()
        
    # Delete workspace before owner (workspace has FK to owner)
    await db_session.delete(ws)
    await db_session.flush()
    
    # Now safe to delete owner
    await db_session.delete(owner)
    await db_session.commit()
