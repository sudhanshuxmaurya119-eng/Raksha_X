from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import or_, select
from passlib.context import CryptContext
from datetime import datetime, timedelta
from jose import jwt, JWTError
import secrets

from database import get_db
from models.user import User
from schemas.user import UserCreate, UserResponse, Token, LoginRequest, GoogleLoginRequest
from config import settings
from auth_dependencies import get_current_user

router = APIRouter()
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt

@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user

@router.post("/register", response_model=UserResponse)
async def register(user: UserCreate, db: AsyncSession = Depends(get_db)):
    clean_email = user.email.strip().lower()
    clean_username = user.username.strip()
    clean_phone = user.phone.strip() if user.phone else None

    result = await db.execute(select(User).where(User.email.ilike(clean_email)))
    if result.scalars().first():
        raise HTTPException(status_code=400, detail="Email is already registered")
    
    result = await db.execute(select(User).where(User.username.ilike(clean_username)))
    if result.scalars().first():
        raise HTTPException(status_code=400, detail="Username is already taken")
        
    db_user = User(
        username=clean_username,
        email=clean_email,
        password_hash=get_password_hash(user.password),
        phone=clean_phone,
        age=user.age
    )
    db.add(db_user)
    await db.commit()
    await db.refresh(db_user)
    return db_user

@router.post("/login", response_model=Token)
async def login(req: LoginRequest, db: AsyncSession = Depends(get_db)):
    clean_id = req.email.strip()
    clean_password = req.password

    conditions = [
        User.email.ilike(clean_id),
        User.username.ilike(clean_id),
        User.phone == clean_id,
    ]
    digits = "".join(filter(str.isdigit, clean_id))
    if len(digits) >= 10:
        ten_digits = digits[-10:]
        conditions.append(User.phone.like(f"%{ten_digits}"))

    result = await db.execute(select(User).where(or_(*conditions)))
    user = result.scalars().first()
    if not user or not verify_password(clean_password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email, username, phone or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer", "user": user}


@router.post("/google", response_model=Token)
async def google_login(req: GoogleLoginRequest, db: AsyncSession = Depends(get_db)):
    try:
        from google.auth.transport.requests import Request
        from google.oauth2.id_token import verify_firebase_token

        claims = verify_firebase_token(
            req.id_token,
            Request(),
            audience=settings.FIREBASE_PROJECT_ID,
        )
    except Exception as error:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Firebase ID token",
        ) from error

    email = (claims.get("email") or "").strip().lower()
    phone = (claims.get("phone_number") or "").strip() or None
    if not email and not phone:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Firebase token must include an email or phone number",
        )

    user = None
    if email:
        user = await db.scalar(select(User).where(User.email == email))
    if user is None and phone:
        user = await db.scalar(select(User).where(User.phone == phone))

    if user is None:
        firebase_uid = (claims.get("user_id") or claims.get("sub") or secrets.token_hex(8)).strip()
        if not email:
            email = f"firebase-phone-{firebase_uid}@rakshax.local"

        base_username = (
            claims.get("name")
            or (email.partition("@")[0] if email else None)
            or phone
            or "RakshaX user"
        ).strip()[:50]
        base_username = base_username or "RakshaX user"
        username = base_username
        suffix = 1
        while await db.scalar(select(User.id).where(User.username == username)):
            suffix_text = f"-{suffix}"
            username = f"{base_username[:50 - len(suffix_text)]}{suffix_text}"
            suffix += 1

        user = User(
            username=username,
            email=email,
            phone=phone,
            password_hash=get_password_hash(secrets.token_urlsafe(32)),
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
    elif phone and not user.phone:
        user.phone = phone
        await db.commit()
        await db.refresh(user)

    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer", "user": user}
