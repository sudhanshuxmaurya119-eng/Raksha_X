from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class UserCreate(BaseModel):
    username: str
    email: str
    password: str
    age: int = Field(default=25, ge=1, le=120)
    phone: Optional[str] = None

class UserResponse(BaseModel):
    id: str
    username: str
    email: str
    phone: Optional[str] = None
    age: Optional[int] = None
    created_at: datetime
    model_config = {"from_attributes": True}

class Token(BaseModel):
    access_token: str
    token_type: str
    user: UserResponse

class LoginRequest(BaseModel):
    email: str
    password: str

class GoogleLoginRequest(BaseModel):
    id_token: str
