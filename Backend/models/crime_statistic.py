import uuid
from datetime import date, datetime
from typing import Optional

from sqlalchemy import Date, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class CrimeStatistic(Base):
    """Validated aggregate crime data. This table intentionally has no incident coordinates."""

    __tablename__ = "crime_statistics"
    __table_args__ = (
        UniqueConstraint(
            "reporting_year", "state", "normalized_district", "police_station",
            "crime_category", "geographic_level", "source_name",
            name="uq_crime_statistic_source_row",
        ),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    data_import_id: Mapped[Optional[str]] = mapped_column(ForeignKey("data_imports.id"), nullable=True, index=True)
    reporting_year: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    reporting_period: Mapped[Optional[str]] = mapped_column(String(80), nullable=True)
    state: Mapped[str] = mapped_column(String(120), nullable=False, index=True)
    district_as_reported: Mapped[str] = mapped_column(String(160), nullable=False)
    normalized_district: Mapped[str] = mapped_column(String(160), nullable=False, index=True)
    police_station: Mapped[Optional[str]] = mapped_column(String(160), nullable=True, index=True)
    crime_category: Mapped[str] = mapped_column(String(200), nullable=False, index=True)
    legal_section: Mapped[Optional[str]] = mapped_column(String(160), nullable=True)
    case_count: Mapped[int] = mapped_column(Integer, nullable=False)
    geographic_level: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    source_name: Mapped[str] = mapped_column(String(200), nullable=False)
    source_url: Mapped[str] = mapped_column(Text, nullable=False)
    source_publication_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    imported_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
    data_quality_status: Mapped[str] = mapped_column(String(32), default="validated", nullable=False)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
