from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class FacilityResponse(BaseModel):
    id: str
    name: str
    facility_type: str  # police, ngo, hospital, fire_station, government_center
    address: str
    phone: Optional[str] = None
    latitude: float
    longitude: float
    services: List[str] = Field(default_factory=list)
    emergency_available: bool = True
    verified: bool = True
    area: Optional[str] = None
    distance_km: Optional[float] = None

    model_config = {"from_attributes": True}

class FacilityCreate(BaseModel):
    name: str
    facility_type: str
    address: str
    phone: Optional[str] = None
    latitude: float
    longitude: float
    services: Optional[str] = None
    emergency_available: bool = True
    verified: bool = True
    area: Optional[str] = None

class RiskAreaResponse(BaseModel):
    id: str
    name: str
    center_lat: float
    center_lng: float
    radius_meters: float
    risk_level: str  # LOW, MEDIUM, HIGH
    registered_cases: int
    cases_this_month: int
    top_category: str
    safety_score: int  # 0 to 100
    nearby_police_count: int
    nearby_ngo_count: int
    nearby_hospital_count: int

class SafetyCaseSummary(BaseModel):
    case_id: str
    latitude: float
    longitude: float
    category: str
    severity: str
    status: str
    created_at: str
    area: str

class MapConfigResponse(BaseModel):
    low_max_cases: int = 5
    medium_max_cases: int = 15
    high_min_cases: int = 16

class MapConfigUpdate(BaseModel):
    low_max_cases: int
    medium_max_cases: int
    high_min_cases: int
