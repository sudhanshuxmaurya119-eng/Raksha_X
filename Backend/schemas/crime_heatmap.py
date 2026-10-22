from datetime import date, datetime
from typing import Any, Literal, Optional

from pydantic import BaseModel, Field


class CrimeStatisticResponse(BaseModel):
    id: str
    reporting_year: int
    reporting_period: Optional[str] = None
    state: str
    district_as_reported: str
    normalized_district: str
    police_station: Optional[str] = None
    crime_category: str
    legal_section: Optional[str] = None
    case_count: int
    geographic_level: str
    source_name: str
    source_url: str
    source_publication_date: Optional[date] = None
    imported_at: datetime
    data_quality_status: str
    notes: Optional[str] = None

    model_config = {"from_attributes": True}


class GeoJsonFeatureCollection(BaseModel):
    type: Literal["FeatureCollection"] = "FeatureCollection"
    features: list[dict[str, Any]] = Field(default_factory=list)
    metadata: dict[str, Any] = Field(default_factory=dict)


class PoliceStationResponse(BaseModel):
    id: str
    official_name: str
    normalized_name: str
    address: Optional[str] = None
    latitude: float
    longitude: float
    public_phone: Optional[str] = None
    source_url: str
    verification_status: str
    last_verified_at: Optional[datetime] = None
    operating_hours: Optional[str] = None

    model_config = {"from_attributes": True}


class SupportServiceResponse(BaseModel):
    id: str
    name: str
    service_type: str
    description: Optional[str] = None
    address: str
    latitude: float
    longitude: float
    public_phone: Optional[str] = None
    official_website: Optional[str] = None
    services_offered: list[str] = Field(default_factory=list)
    operating_hours: Optional[str] = None
    eligibility_notes: Optional[str] = None
    verification_status: str
    verification_source: str
    last_verified_at: Optional[datetime] = None
    distance_km: Optional[float] = None


class ImportReport(BaseModel):
    import_id: str
    filename: str
    status: str
    total_rows: int
    accepted_rows: int
    rejected_rows: int
    duplicate_rows: int
    headers: list[str]
    preview: list[dict[str, Any]]
    errors: list[str] = Field(default_factory=list)
    missing_columns: list[str] = Field(default_factory=list)
