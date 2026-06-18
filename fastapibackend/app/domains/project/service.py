from typing import List
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.domains.project.models import Project
from app.domains.project.schemas import ProjectCreate

async def get_projects_by_workspace(db: AsyncSession, workspace_id: UUID) -> List[Project]:
    result = await db.execute(select(Project).filter(Project.workspace_id == workspace_id, Project.is_archived == False))
    return result.scalars().all()

async def create_project(db: AsyncSession, project_in: ProjectCreate, user_id: UUID) -> Project:
    db_obj = Project(
        name=project_in.name,
        description=project_in.description,
        color=project_in.color,
        icon=project_in.icon,
        workspace_id=project_in.workspace_id,
        team_id=project_in.team_id,
        created_by=user_id,
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj
