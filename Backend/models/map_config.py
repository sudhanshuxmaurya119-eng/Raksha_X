from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column
from database import Base

class MapConfig(Base):
    __tablename__ = "map_configs"
    key: Mapped[str] = mapped_column(String(50), primary_key=True, default="default")
    low_max_cases: Mapped[int] = mapped_column(Integer, default=5)
    medium_max_cases: Mapped[int] = mapped_column(Integer, default=15)
    high_min_cases: Mapped[int] = mapped_column(Integer, default=16)
