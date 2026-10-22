import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import DateTime, Float, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class SupportService(Base):
    __tablename__ = "support_services"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    service_type: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    address: Mapped[str] = mapped_column(Text, nullable=False)
    latitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    longitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    public_phone: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    official_website: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    services_offered: Mapped[str] = mapped_column(Text, default="", nullable=False)
    operating_hours: Mapped[Optional[str]] = mapped_column(String(160), nullable=True)
    eligibility_notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    verification_status: Mapped[str] = mapped_column(String(32), default="pending", nullable=False, index=True)
    verification_source: Mapped[str] = mapped_column(Text, nullable=False)
    last_verified_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
