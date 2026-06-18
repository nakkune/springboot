import os
import uuid
import shutil
from typing import List, Optional
from uuid import UUID
from fastapi import UploadFile
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.domains.attachment.models import Attachment
from app.domains.user.models import User
from app.domains.item.models import Item
from app.domains.board.models import Board
from app.domains.project.models import Project
from app.domains.extra.models import ActivityLog

UPLOAD_DIR = os.path.join(os.getcwd(), "uploads")

async def get_attachments_by_item(db: AsyncSession, item_id: UUID) -> List[Attachment]:
    result = await db.execute(
        select(Attachment, User.full_name)
        .join(User, Attachment.uploader_id == User.id)
        .filter(Attachment.item_id == item_id)
        .order_by(Attachment.created_at.desc())
    )
    
    attachments = []
    for attachment, full_name in result.all():
        attachment.uploader_name = full_name
        attachments.append(attachment)
    return attachments

async def upload_attachment(
    db: AsyncSession,
    item_id: UUID,
    uploader_id: UUID,
    file: UploadFile
) -> Attachment:
    # Ensure upload directory exists
    os.makedirs(UPLOAD_DIR, exist_ok=True)

    # Clean filename and generate a unique one
    original_filename = file.filename if file.filename else "unknown"
    unique_filename = f"{uuid.uuid4()}_{original_filename}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)

    # Copy the file to local uploads directory
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Calculate file size
    file_size = os.path.getsize(file_path)

    # Web url path
    storage_url = f"/uploads/{unique_filename}"

    # Save metadata to DB
    db_obj = Attachment(
        item_id=item_id,
        uploader_id=uploader_id,
        file_name=original_filename,
        file_size=file_size,
        mime_type=file.content_type,
        storage_url=storage_url
    )
    db.add(db_obj)
    await db.flush()

    # Log activity for the item
    # 10년 차 시니어 설계: item의 board, project, workspace 정보를 추적하여 로그의 계층 무결성 유지
    item_info = await db.execute(
        select(Item.board_id, Board.project_id, Project.workspace_id)
        .join(Board, Item.board_id == Board.id)
        .join(Project, Board.project_id == Project.id)
        .filter(Item.id == item_id)
    )
    row = item_info.first()
    if row:
        board_id, project_id, workspace_id = row
    else:
        board_id, project_id, workspace_id = None, None, None

    activity = ActivityLog(
        workspace_id=workspace_id,
        project_id=project_id,
        board_id=board_id,
        item_id=item_id,
        actor_id=uploader_id,
        action="attachment.add",
        meta={"fileName": original_filename, "fileSize": file_size}
    )
    db.add(activity)
    await db.commit()
    await db.refresh(db_obj)

    # Fetch uploader name for the response DTO
    user = await db.get(User, uploader_id)
    db_obj.uploader_name = user.full_name if user else "Unknown"

    return db_obj

async def save_profile_image(file: UploadFile) -> str:
    # Ensure upload directory exists
    os.makedirs(UPLOAD_DIR, exist_ok=True)

    original_filename = file.filename if file.filename else "avatar.png"
    unique_filename = f"{uuid.uuid4()}_{original_filename}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Return storage URL compatible with frontend
    return f"/api/uploads/{unique_filename}"
