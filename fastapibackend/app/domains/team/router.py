from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.team import schemas, service

router = APIRouter()

@router.get("/workspace/{workspace_id}", response_model=List[schemas.Team])
async def get_teams_by_workspace_id(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    특정 워크스페이스에 속한 모든 팀 목록을 조회합니다.
    """
    teams = await service.get_teams_by_workspace(db, workspace_id=workspace_id)
    return teams

@router.get("/{id}", response_model=schemas.Team)
async def get_team_by_id(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    특정 팀의 세부 정보를 조회합니다.
    """
    team = await service.get_team(db, team_id=id)
    if not team:
        raise HTTPException(status_code=404, detail=f"팀을 찾을 수 없습니다. ID: {id}")
    return team

@router.post("", response_model=schemas.Team, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=schemas.Team, status_code=status.HTTP_201_CREATED, include_in_schema=False)
async def create_team(
    *,
    db: AsyncSession = Depends(get_db),
    team_in: schemas.TeamCreate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    새로운 팀을 생성합니다.
    """
    team = await service.create_team(db, team_in=team_in, user_id=current_user.id)
    return team

@router.put("/{id}", response_model=schemas.Team)
async def update_team(
    *,
    id: UUID,
    db: AsyncSession = Depends(get_db),
    team_in: schemas.TeamUpdate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    기존 팀 정보를 수정합니다.
    """
    db_obj = await service.get_team(db, team_id=id)
    if not db_obj:
        raise HTTPException(status_code=404, detail=f"팀을 찾을 수 없습니다. ID: {id}")
    team = await service.update_team(db, db_obj=db_obj, obj_in=team_in)
    return team

@router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_team(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    특정 팀을 삭제합니다.
    """
    db_obj = await service.get_team(db, team_id=id)
    if not db_obj:
        raise HTTPException(status_code=404, detail=f"팀을 찾을 수 없습니다. ID: {id}")
    await service.delete_team(db, id=id)
    return None
