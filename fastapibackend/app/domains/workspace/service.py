from typing import List, Optional
from uuid import UUID, uuid4
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.domains.workspace.models import Workspace, WorkspaceMember
from app.domains.workspace.schemas import WorkspaceCreate, WorkspaceUpdate, WorkspaceMemberCreate
from app.domains.user.models import User
from app.core.security import get_password_hash

async def get_workspace(db: AsyncSession, workspace_id: UUID) -> Optional[Workspace]:
    result = await db.execute(select(Workspace).filter(Workspace.id == workspace_id))
    return result.scalars().first()

async def get_workspaces_by_user(db: AsyncSession, user_id: UUID) -> List[Workspace]:
    result = await db.execute(
        select(Workspace)
        .join(WorkspaceMember)
        .filter(WorkspaceMember.user_id == user_id)
    )
    return result.scalars().all()

async def create_workspace(db: AsyncSession, *, workspace_in: WorkspaceCreate, owner_id: UUID) -> Workspace:
    db_obj = Workspace(
        name=workspace_in.name,
        slug=workspace_in.slug,
        logo_url=workspace_in.logo_url,
        plan=workspace_in.plan,
        owner_id=owner_id
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    
    # Add owner as a member
    member_obj = WorkspaceMember(
        workspace_id=db_obj.id,
        user_id=owner_id,
        role="owner"
    )
    db.add(member_obj)
    await db.commit()
    
    return db_obj

async def update_workspace(db: AsyncSession, *, db_obj: Workspace, obj_in: WorkspaceUpdate) -> Workspace:
    update_data = obj_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_workspace(db: AsyncSession, *, id: UUID) -> Workspace:
    obj = await db.get(Workspace, id)
    if obj:
        await db.delete(obj)
        await db.commit()
    return obj

async def get_workspace_members(db: AsyncSession, workspace_id: UUID) -> List[WorkspaceMember]:
    result = await db.execute(
        select(WorkspaceMember, User.email, User.full_name, User.avatar_url)
        .join(User, WorkspaceMember.user_id == User.id)
        .filter(WorkspaceMember.workspace_id == workspace_id)
    )
    members = []
    for row in result.all():
        member, email, full_name, avatar_url = row
        member.email = email
        member.full_name = full_name
        member.avatar_url = avatar_url
        members.append(member)
    return members

async def add_workspace_member(db: AsyncSession, *, workspace_id: UUID, member_in: WorkspaceMemberCreate, invited_by: UUID) -> WorkspaceMember:
    db_obj = WorkspaceMember(
        id=uuid4(),
        workspace_id=workspace_id,
        user_id=member_in.user_id,
        role=member_in.role,
        invited_by=invited_by
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    
    # Populate fields
    user = await db.get(User, db_obj.user_id)
    if user:
        db_obj.email = user.email
        db_obj.full_name = user.full_name
        db_obj.avatar_url = user.avatar_url
        
    return db_obj

async def invite_workspace_member(
    db: AsyncSession,
    *,
    workspace_id: UUID,
    email: str,
    role: str = "member",
    invited_by: UUID
) -> WorkspaceMember:
    email_clean = email.strip()
    if not email_clean:
        raise ValueError("올바른 이메일 주소를 입력해 주세요.")
        
    # 1. Check if user exists
    user_res = await db.execute(select(User).filter(User.email == email_clean))
    user = user_res.scalars().first()
    
    if not user:
        # Create user
        name_part = email_clean.split("@")[0]
        full_name = name_part[0].upper() + name_part[1:] if len(name_part) > 1 else name_part.upper()
        
        user = User(
            id=uuid4(),
            email=email_clean,
            password_hash=get_password_hash("1234"),
            full_name=full_name,
            is_active=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
        
    # 2. Check if already a member
    exist_res = await db.execute(
        select(WorkspaceMember).filter(
            WorkspaceMember.workspace_id == workspace_id,
            WorkspaceMember.user_id == user.id
        )
    )
    if exist_res.scalars().first():
        raise ValueError("이미 해당 워크스페이스의 멤버로 등록된 사용자입니다.")
        
    # 3. Add to workspace
    member = WorkspaceMember(
        id=uuid4(),
        workspace_id=workspace_id,
        user_id=user.id,
        role=role,
        invited_by=invited_by
    )
    db.add(member)
    await db.commit()
    await db.refresh(member)
    
    # Populate fields
    member.email = user.email
    member.full_name = user.full_name
    member.avatar_url = user.avatar_url
    
    return member

async def update_workspace_member_role(db: AsyncSession, *, member_id: UUID, role: str) -> Optional[WorkspaceMember]:
    role_clean = role.strip()
    if not role_clean:
        raise ValueError("올바른 역할을 입력해 주세요.")
    db_obj = await db.get(WorkspaceMember, member_id)
    if db_obj:
        db_obj.role = role_clean
        db.add(db_obj)
        await db.commit()
        await db.refresh(db_obj)
        
        # Populate user info
        user = await db.get(User, db_obj.user_id)
        if user:
            db_obj.email = user.email
            db_obj.full_name = user.full_name
            db_obj.avatar_url = user.avatar_url
    return db_obj

async def delete_workspace_member(db: AsyncSession, *, member_id: UUID) -> Optional[WorkspaceMember]:
    db_obj = await db.get(WorkspaceMember, member_id)
    if db_obj:
        await db.delete(db_obj)
        await db.commit()
    return db_obj
