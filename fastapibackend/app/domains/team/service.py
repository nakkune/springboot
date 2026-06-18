from typing import List, Optional
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.domains.team.models import Team, TeamMember
from app.domains.team.schemas import TeamCreate, TeamUpdate

async def get_teams_by_workspace(db: AsyncSession, workspace_id: UUID) -> List[Team]:
    result = await db.execute(
        select(Team)
        .filter(Team.workspace_id == workspace_id)
        .order_by(Team.created_at.desc())
    )
    return list(result.scalars().all())

async def get_team(db: AsyncSession, team_id: UUID) -> Optional[Team]:
    result = await db.execute(select(Team).filter(Team.id == team_id))
    return result.scalars().first()

async def create_team(db: AsyncSession, *, team_in: TeamCreate, user_id: UUID) -> Team:
    db_obj = Team(
        workspace_id=team_in.workspace_id,
        name=team_in.name,
        description=team_in.description,
        color=team_in.color,
        created_by=user_id
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_team(db: AsyncSession, *, db_obj: Team, obj_in: TeamUpdate) -> Team:
    update_data = obj_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_team(db: AsyncSession, *, id: UUID) -> Optional[Team]:
    obj = await db.get(Team, id)
    if obj:
        await db.delete(obj)
        await db.commit()
    return obj
