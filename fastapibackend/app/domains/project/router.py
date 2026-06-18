from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.project import schemas, service

router = APIRouter()

@router.get("/workspace/{workspace_id}", response_model=List[schemas.Project])
async def read_projects(
    workspace_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Retrieve projects for a workspace.
    """
    projects = await service.get_projects_by_workspace(db, workspace_id=workspace_id)
    return projects

@router.post("/", response_model=schemas.Project)
async def create_project(
    *,
    db: AsyncSession = Depends(get_db),
    project_in: schemas.ProjectCreate,
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Create new project.
    """
    project = await service.create_project(db, project_in=project_in, user_id=current_user.id)
    return project
