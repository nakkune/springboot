from datetime import timedelta
from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.security import create_access_token, verify_password
from app.api.dependencies import get_db, get_current_active_user
from app.domains.user import schemas, service
from app.domains.user.models import User

router = APIRouter()

@router.post("/login/access-token", response_model=schemas.Token)
async def login_access_token(
    db: AsyncSession = Depends(get_db), form_data: OAuth2PasswordRequestForm = Depends()
) -> Any:
    """
    OAuth2 compatible token login, get an access token for future requests
    """
    user = await service.get_user_by_email(db, email=form_data.username)
    if not user or not verify_password(form_data.password, user.password_hash):
        raise HTTPException(status_code=400, detail="Incorrect email or password")
    elif not user.is_active:
        raise HTTPException(status_code=400, detail="Inactive user")
    
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    return {
        "access_token": create_access_token(
            user.id, expires_delta=access_token_expires
        ),
        "token_type": "bearer",
    }

@router.post("/login")
async def login_json(
    *,
    db: AsyncSession = Depends(get_db),
    login_in: schemas.UserLogin,
) -> Any:
    """
    JSON login for frontend
    """
    user = await service.get_user_by_email(db, email=login_in.email)
    if not user or not verify_password(login_in.password, user.password_hash):
        return {"success": False, "error": "이메일 또는 비밀번호가 올바르지 않습니다."}
    elif not user.is_active:
        return {"success": False, "error": "비활성화된 계정입니다."}
    
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    token = create_access_token(user.id, expires_delta=access_token_expires)
    
    return {
        "success": True,
        "token": token,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "fullName": user.full_name,
            "avatarUrl": user.avatar_url,
            "role": user.role,
            "memberStatus": user.member_status,
            "theme": user.theme
        }
    }

@router.post("/", response_model=schemas.User)
async def create_user(
    *,
    db: AsyncSession = Depends(get_db),
    user_in: schemas.UserCreate,
) -> Any:
    """
    Create new user.
    """
    user = await service.get_user_by_email(db, email=user_in.email)
    if user:
        raise HTTPException(
            status_code=400,
            detail="The user with this username already exists in the system.",
        )
    user = await service.create_user(db, user_in=user_in)
    return user

@router.get("/me", response_model=schemas.User)
async def read_user_me(
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Get current user.
    """
    return current_user

@router.get("", response_model=List[schemas.User])
@router.get("/", response_model=List[schemas.User])
async def read_users(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Get list of users. STUB.
    """
    return [current_user]

@router.put("/{user_id}", response_model=schemas.User)
async def update_user(
    user_id: UUID,
    user_in: schemas.UserUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    Update user preferences.
    """
    if current_user.id != user_id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized to update this user")
    
    from sqlalchemy.future import select
    res = await db.execute(select(User).filter(User.id == user_id))
    user = res.scalars().first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    return await service.update_user(db, db_obj=user, obj_in=user_in)
