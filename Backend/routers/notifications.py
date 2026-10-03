from datetime import datetime

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from database import get_db
from auth_dependencies import get_current_user
from models.user import User
from models.push_token import PushToken

router = APIRouter()


class PushTokenRequest(BaseModel):
    user_id: str
    token: str
    platform: str = "android"


@router.post("/token")
async def register_token(
    request: PushTokenRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(PushToken).where(
            PushToken.user_id == current_user.id,
            PushToken.token == request.token
        )
    )
    token = result.scalars().first()
    if token:
        token.platform = request.platform
        token.updated_at = datetime.utcnow()
    else:
        db.add(PushToken(
            user_id=current_user.id,
            token=request.token,
            platform=request.platform,
        ))
    await db.commit()
    return {"status": "registered"}


@router.delete("/token")
async def unregister_token(
    request: PushTokenRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    await db.execute(
        delete(PushToken).where(
            PushToken.user_id == current_user.id,
            PushToken.token == request.token
        )
    )
    await db.commit()
    return {"status": "removed"}
