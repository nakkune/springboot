import os
import uuid
import shutil
from typing import Any, List, Optional
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, File, Form, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.attachment import schemas, service

router = APIRouter()
files_router = APIRouter()

@router.get("/item/{itemId}", response_model=List[schemas.Attachment])
async def get_attachments_by_item_id(
    itemId: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    특정 아이템 ID에 해당하는 모든 첨부파일 목록을 조회합니다.
    """
    attachments = await service.get_attachments_by_item(db, item_id=itemId)
    return attachments

@router.post("/upload", response_model=schemas.Attachment, status_code=status.HTTP_201_CREATED)
async def upload_attachment(
    itemId: UUID = Form(...),
    uploaderId: Optional[str] = Form(None),
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """
    특정 아이템에 대한 첨부파일을 업로드합니다.
    """
    placeholder_id = "f1b9b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
    final_uploader_id = current_user.id
    if uploaderId and uploaderId.strip() and uploaderId != placeholder_id:
        try:
            final_uploader_id = UUID(uploaderId)
        except ValueError:
            pass

    attachment = await service.upload_attachment(
        db,
        item_id=itemId,
        uploader_id=final_uploader_id,
        file=file
    )
    return attachment

@router.post("/upload/profile", status_code=status.HTTP_201_CREATED)
async def upload_profile_image(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """
    프로필 이미지 파일을 업로드하고 저장된 URL을 반환합니다.
    """
    url = await service.save_profile_image(file)
    return {"url": url}


# Files Router for /files/upload
@files_router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """
    단일 파일을 로컬 스토리지에 업로드하고 파일 정보를 반환합니다.
    """
    os.makedirs(service.UPLOAD_DIR, exist_ok=True)
    original_filename = file.filename if file.filename else "unknown"
    unique_filename = f"{uuid.uuid4()}_{original_filename}"
    file_path = os.path.join(service.UPLOAD_DIR, unique_filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    file_size = os.path.getsize(file_path)
    url = f"/uploads/{unique_filename}"

    return {
        "originalName": original_filename,
        "url": url,
        "mimeType": file.content_type,
        "size": str(file_size)
    }
