import uuid
from datetime import datetime
from sqlalchemy import String, Float, Boolean, DateTime, Text
from sqlalchemy.orm import Mapped, mapped_column
from typing import Optional
from database import Base

class Facility(Base):
    __tablename__ = "facilities"
    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    facility_type: Mapped[str] = mapped_column(String(50), nullable=False)  # police, ngo, hospital, fire_station, government_center
    address: Mapped[str] = mapped_column(String(255), nullable=False)
    phone: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    services: Mapped[Optional[str]] = mapped_column(Text, nullable=True)  # e.g. "Women Safety, Legal Aid, 24x7 Emergency"
    emergency_available: Mapped[bool] = mapped_column(Boolean, default=True)
    verified: Mapped[bool] = mapped_column(Boolean, default=True)
    area: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
