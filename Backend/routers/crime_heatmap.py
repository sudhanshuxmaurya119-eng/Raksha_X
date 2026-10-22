import json
import math
from collections import defaultdict
from datetime import datetime
from typing import Literal, Optional

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from auth_dependencies import require_safety_data_admin
from database import get_db
from models.crime_statistic import CrimeStatistic
from models.data_import import DataImport
from models.police_station import PoliceStation
from models.support_service import SupportService
from models.user import User
from schemas.crime_heatmap import (
    CrimeStatisticResponse, GeoJsonFeatureCollection, ImportReport,
    PoliceStationResponse, SupportServiceResponse,
)
from services.crime_data_service import import_crime_file, normalize_name

router = APIRouter()
admin_router = APIRouter()
MAX_IMPORT_BYTES = 10 * 1024 * 1024


def _apply_crime_filters(query, year: int | None, start_year: int | None, end_year: int | None,
                         category: str | None, district: str | None, geographic_level: str | None):
    if year is not None:
        query = query.where(CrimeStatistic.reporting_year == year)
    if start_year is not None:
        query = query.where(CrimeStatistic.reporting_year >= start_year)
    if end_year is not None:
        query = query.where(CrimeStatistic.reporting_year <= end_year)
    if category:
        query = query.where(CrimeStatistic.crime_category.ilike(f"%{category.strip()}%"))
    if district:
        query = query.where(CrimeStatistic.normalized_district == normalize_name(district))
    if geographic_level:
        query = query.where(CrimeStatistic.geographic_level == geographic_level)
    return query


def _distance_km(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    radius = 6371.0088
    d_lat, d_lng = math.radians(lat2 - lat1), math.radians(lng2 - lng1)
    a = math.sin(d_lat / 2) ** 2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(d_lng / 2) ** 2
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


@router.get("/crime-statistics", response_model=list[CrimeStatisticResponse])
async def crime_statistics(
    year: Optional[int] = Query(None, ge=1900, le=2100), start_year: Optional[int] = Query(None, ge=1900, le=2100),
    end_year: Optional[int] = Query(None, ge=1900, le=2100), category: Optional[str] = Query(None, max_length=200),
    district: Optional[str] = Query(None, max_length=160), geographic_level: Optional[str] = Query(None),
    limit: int = Query(500, ge=1, le=1000), offset: int = Query(0, ge=0), db: AsyncSession = Depends(get_db),
):
    if start_year and end_year and start_year > end_year:
        raise HTTPException(422, "start_year cannot be greater than end_year")
    query = _apply_crime_filters(select(CrimeStatistic).order_by(CrimeStatistic.reporting_year.desc()), year, start_year, end_year, category, district, geographic_level)
    return (await db.execute(query.offset(offset).limit(limit))).scalars().all()


@router.get("/heatmap", response_model=GeoJsonFeatureCollection)
async def heatmap(
    year: Optional[int] = Query(None, ge=1900, le=2100), category: Optional[str] = Query(None, max_length=200),
    metric: Literal["case_count", "yoy_change", "yoy_percent", "rate_per_100k"] = "case_count",
    geographic_level: Literal["district", "police_station"] = "district", db: AsyncSession = Depends(get_db),
):
    if metric == "rate_per_100k":
        raise HTTPException(422, "Rate per 100,000 is unavailable because no validated population denominator has been imported.")
    if geographic_level == "police_station":
        # Stations are not incident locations; station-level aggregates are only renderable with an explicit verified station mapping.
        raise HTTPException(422, "Police-station heatmaps require validated station statistics and verified station coordinates; no privacy-safe incident density is inferred.")
    query = _apply_crime_filters(select(CrimeStatistic), year, None, None, category, None, "district")
    records = (await db.execute(query)).scalars().all()
    totals: dict[tuple[str, int], int] = defaultdict(int)
    for record in records:
        totals[(record.normalized_district, record.reporting_year)] += record.case_count
    aggregates = []
    for (district, record_year), total in sorted(totals.items()):
        previous = totals.get((district, record_year - 1))
        value = total
        if metric == "yoy_change":
            if previous is None:
                continue
            value = total - previous
        elif metric == "yoy_percent":
            if previous in (None, 0):
                continue
            value = round(((total - previous) / previous) * 100, 2)
        aggregates.append({"district": district, "year": record_year, "value": value, "case_count": total})
    # No administrative boundary dataset has been verified/imported. Returning no geometry avoids pretending totals are incident locations.
    return GeoJsonFeatureCollection(
        features=[],
        metadata={
            "geographic_level": "district", "metric": metric, "aggregates": aggregates,
            "message": "District aggregates are available, but no verified district-boundary GeoJSON has been imported. No geographic heat layer is drawn.",
        },
    )


@router.get("/police-stations", response_model=list[PoliceStationResponse])
async def police_stations(db: AsyncSession = Depends(get_db)):
    query = select(PoliceStation).where(
        PoliceStation.verification_status == "verified", PoliceStation.latitude.is_not(None), PoliceStation.longitude.is_not(None)
    ).order_by(PoliceStation.official_name)
    return (await db.execute(query)).scalars().all()


@router.get("/support-services", response_model=list[SupportServiceResponse])
async def support_services(
    latitude: Optional[float] = Query(None, ge=-90, le=90), longitude: Optional[float] = Query(None, ge=-180, le=180),
    radius_km: float = Query(15, gt=0, le=100), service_type: Optional[str] = Query(None, max_length=80),
    db: AsyncSession = Depends(get_db),
):
    if (latitude is None) != (longitude is None):
        raise HTTPException(422, "latitude and longitude must be supplied together")
    query = select(SupportService).where(
        SupportService.verification_status == "verified", SupportService.latitude.is_not(None), SupportService.longitude.is_not(None)
    )
    if service_type:
        query = query.where(SupportService.service_type == service_type.strip())
    records = (await db.execute(query.order_by(SupportService.name))).scalars().all()
    response = []
    for service in records:
        distance = _distance_km(latitude, longitude, service.latitude, service.longitude) if latitude is not None else None
        if distance is not None and distance > radius_km:
            continue
        response.append(SupportServiceResponse(
            id=service.id, name=service.name, service_type=service.service_type, description=service.description,
            address=service.address, latitude=service.latitude, longitude=service.longitude, public_phone=service.public_phone,
            official_website=service.official_website, services_offered=[s.strip() for s in service.services_offered.split(",") if s.strip()],
            operating_hours=service.operating_hours, eligibility_notes=service.eligibility_notes,
            verification_status=service.verification_status, verification_source=service.verification_source,
            last_verified_at=service.last_verified_at, distance_km=round(distance, 2) if distance is not None else None,
        ))
    return sorted(response, key=lambda item: item.distance_km if item.distance_km is not None else -1)


@router.get("/data-sources")
async def data_sources(db: AsyncSession = Depends(get_db)):
    imports = (await db.execute(select(DataImport).order_by(DataImport.imported_at.desc()))).scalars().all()
    sources = []
    for item in imports:
        sources.append({"name": item.source_name, "source_url": item.source_url, "last_imported_at": item.imported_at,
                        "total_rows": item.total_rows, "accepted_rows": item.accepted_rows, "data_quality": item.status})
    return {"sources": sources, "official_references": [
        {"name": "NCRB district-wise crimes against women", "url": "https://www.data.gov.in/catalog/district-wise-crimes-committed-against-women"},
        {"name": "NCRB Crime in India 2023", "url": "https://www.data.gov.in/catalog/crime-india-2023"},
        {"name": "Meerut Police Station List", "url": "https://meerut.nic.in/police-station-list/"},
    ]}


@router.get("/filter-options")
async def filter_options(db: AsyncSession = Depends(get_db)):
    years = (await db.execute(select(CrimeStatistic.reporting_year).distinct().order_by(CrimeStatistic.reporting_year.desc()))).scalars().all()
    categories = (await db.execute(select(CrimeStatistic.crime_category).distinct().order_by(CrimeStatistic.crime_category))).scalars().all()
    levels = (await db.execute(select(CrimeStatistic.geographic_level).distinct().order_by(CrimeStatistic.geographic_level))).scalars().all()
    return {"years": years, "categories": categories, "geographic_levels": levels}


@router.get("/overview")
async def overview(
    year: Optional[int] = Query(None, ge=1900, le=2100), category: Optional[str] = Query(None),
    geographic_level: Optional[str] = Query("district"), db: AsyncSession = Depends(get_db),
):
    crime_query = _apply_crime_filters(select(CrimeStatistic), year, None, None, category, None, geographic_level)
    crime_records = (await db.execute(crime_query)).scalars().all()
    verified_stations = await db.scalar(select(func.count()).select_from(PoliceStation).where(PoliceStation.verification_status == "verified"))
    verified_services = await db.scalar(select(func.count()).select_from(SupportService).where(SupportService.verification_status == "verified"))
    return {
        "historical_case_count": sum(row.case_count for row in crime_records),
        "zones": len({row.normalized_district for row in crime_records}),
        "facilities": verified_stations or 0, "services": verified_services or 0,
        "has_historical_data": bool(crime_records),
        "empty_message": None if crime_records else "No verified historical crime data is available for the selected area and year.",
        "support_empty_message": None if (verified_stations or verified_services) else "Support locations have not yet been verified for this area.",
    }


@router.get("/health")
async def safety_data_health(db: AsyncSession = Depends(get_db)):
    try:
        records = await db.scalar(select(func.count()).select_from(CrimeStatistic))
        return {"status": "healthy", "crime_data_available": bool(records), "records": records or 0}
    except Exception:
        return {"status": "degraded", "crime_data_available": False}


@admin_router.post("/import", response_model=ImportReport, status_code=status.HTTP_201_CREATED)
async def import_safety_data(
    file: UploadFile = File(...), source_name: str = Form(..., min_length=2, max_length=200),
    source_url: str = Form(..., min_length=8, max_length=2000), replace_source: bool = Form(False),
    _admin: User = Depends(require_safety_data_admin), db: AsyncSession = Depends(get_db),
):
    filename = file.filename or "upload"
    if not filename.lower().endswith((".csv", ".xlsx", ".xls")):
        raise HTTPException(415, "Only CSV, XLSX, and XLS files are supported")
    raw = await file.read(MAX_IMPORT_BYTES + 1)
    if len(raw) > MAX_IMPORT_BYTES:
        raise HTTPException(413, "Import files must be 10 MB or smaller")
    try:
        return await import_crime_file(db, raw=raw, filename=filename, source_name=source_name, source_url=source_url, replace_source=replace_source)
    except (UnicodeDecodeError, ValueError) as error:
        raise HTTPException(422, str(error)) from error


@admin_router.delete("/imports/{import_id}")
async def rollback_safety_import(import_id: str, _admin: User = Depends(require_safety_data_admin), db: AsyncSession = Depends(get_db)):
    item = await db.scalar(select(DataImport).where(DataImport.id == import_id))
    if not item:
        raise HTTPException(404, "Import not found")
    await db.execute(CrimeStatistic.__table__.delete().where(CrimeStatistic.data_import_id == import_id))
    await db.delete(item)
    await db.commit()
    return {"status": "rolled_back", "import_id": import_id}
