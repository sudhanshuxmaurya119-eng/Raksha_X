import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import DateTime, Float, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class PoliceStation(Base):
    __tablename__ = "police_stations"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    official_name: Mapped[str] = mapped_column(String(160), nullable=False, unique=True)
    normalized_name: Mapped[str] = mapped_column(String(160), nullable=False, index=True)
    address: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    latitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    longitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    public_phone: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    source_url: Mapped[str] = mapped_column(Text, nullable=False)
    verification_status: Mapped[str] = mapped_column(String(32), default="pending", nullable=False, index=True)
    last_verified_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    operating_hours: Mapped[Optional[str]] = mapped_column(String(160), nullable=True)
