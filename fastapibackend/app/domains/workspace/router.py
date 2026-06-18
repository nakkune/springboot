from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, Body
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.workspace import schemas, service

router = APIRouter()

@router.post("", response_model=schemas.Workspace)
@router.post("/", response_model=schemas.Workspace)
async def create_workspace(
    *,
    db: AsyncSession = Depends(get_db),
    workspace_in: schemas.WorkspaceCreate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Create new workspace.
    """
    workspace = await service.create_workspace(db, workspace_in=workspace_in, owner_id=current_user.id)
    return workspace

@router.get("", response_model=List[schemas.Workspace])
@router.get("/", response_model=List[schemas.Workspace])
async def read_workspaces(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve workspaces for current user.
    """
    workspaces = await service.get_workspaces_by_user(db, user_id=current_user.id)
    return workspaces

@router.get("/{workspace_id}", response_model=schemas.Workspace)
async def read_workspace(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Get a specific workspace by id.
    """
    workspace = await service.get_workspace(db, workspace_id=workspace_id)
    if not workspace:
        raise HTTPException(status_code=404, detail="Workspace not found")
    return workspace

@router.put("/{workspace_id}", response_model=schemas.Workspace)
async def update_workspace(
    workspace_id: UUID,
    workspace_in: schemas.WorkspaceUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Update workspace settings.
    """
    workspace = await service.get_workspace(db, workspace_id=workspace_id)
    if not workspace:
        raise HTTPException(status_code=404, detail="Workspace not found")
        
    # Check permissions
    if workspace.owner_id != current_user.id:
        members = await service.get_workspace_members(db, workspace_id=workspace_id)
        is_admin = any(m.user_id == current_user.id and m.role in ["owner", "admin"] for m in members)
        if not is_admin:
            raise HTTPException(status_code=403, detail="Not authorized to edit workspace settings")
            
    return await service.update_workspace(db, db_obj=workspace, obj_in=workspace_in)

@router.get("/{workspace_id}/members", response_model=List[schemas.WorkspaceMember])
async def read_workspace_members(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve members for a workspace.
    """
    members = await service.get_workspace_members(db, workspace_id=workspace_id)
    return members

@router.post("/{workspace_id}/members", response_model=schemas.WorkspaceMember)
async def add_workspace_member(
    workspace_id: UUID,
    member_in: schemas.WorkspaceMemberCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Add a member to a workspace.
    """
    member = await service.add_workspace_member(db, workspace_id=workspace_id, member_in=member_in, invited_by=current_user.id)
    return member

@router.post("/{workspace_id}/members/invite", response_model=schemas.WorkspaceMember)
async def invite_member(
    workspace_id: UUID,
    payload: dict = Body(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Invite a member to the workspace by email.
    """
    email = payload.get("email")
    role = payload.get("role", "member")
    try:
        member = await service.invite_workspace_member(
            db,
            workspace_id=workspace_id,
            email=email,
            role=role,
            invited_by=current_user.id
        )
        return member
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{workspace_id}/members/{member_id}", response_model=schemas.WorkspaceMember)
async def update_member_role(
    workspace_id: UUID,
    member_id: UUID,
    payload: dict = Body(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Update a workspace member's role.
    """
    role = payload.get("role")
    workspace = await service.get_workspace(db, workspace_id=workspace_id)
    if not workspace:
        raise HTTPException(status_code=404, detail="Workspace not found")
        
    members = await service.get_workspace_members(db, workspace_id=workspace_id)
    is_admin = any(m.user_id == current_user.id and m.role in ["owner", "admin"] for m in members)
    if workspace.owner_id != current_user.id and not is_admin:
        raise HTTPException(status_code=403, detail="Not authorized to edit member roles")
        
    try:
        updated = await service.update_workspace_member_role(db, member_id=member_id, role=role)
        if not updated:
            raise HTTPException(status_code=404, detail="Member not found")
        return updated
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{workspace_id}/members/{member_id}", status_code=204, response_model=None)
async def delete_member(
    workspace_id: UUID,
    member_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Remove (kick) a member from the workspace.
    """
    workspace = await service.get_workspace(db, workspace_id=workspace_id)
    if not workspace:
        raise HTTPException(status_code=404, detail="Workspace not found")
        
    members = await service.get_workspace_members(db, workspace_id=workspace_id)
    is_admin = any(m.user_id == current_user.id and m.role in ["owner", "admin"] for m in members)
    if workspace.owner_id != current_user.id and not is_admin:
        raise HTTPException(status_code=403, detail="Not authorized to manage workspace members")
        
    deleted = await service.delete_workspace_member(db, member_id=member_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Member not found")
    return None
