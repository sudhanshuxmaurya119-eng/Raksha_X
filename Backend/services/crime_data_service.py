"""Validation and query helpers for privacy-safe historical crime aggregates."""

import csv
import io
import json
import re
import unicodedata
from datetime import date
from typing import Any

import pandas as pd
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from models.crime_statistic import CrimeStatistic
from models.data_import import DataImport

REQUIRED_INTERNAL_COLUMNS = {"reporting_year", "state", "district_as_reported", "crime_category", "case_count"}
COLUMN_ALIASES = {
    "reporting_year": {"year", "reporting year", "reporting_year", "crime year"},
    "reporting_period": {"period", "reporting period", "reporting_period"},
    "state": {"state", "state/ut", "state ut"},
    "district_as_reported": {"district", "district name", "district_as_reported", "district as reported"},
    "police_station": {"police station", "police_station", "station", "thana"},
    "crime_category": {"crime category", "crime_category", "category", "crime head", "crime type"},
    "legal_section": {"legal section", "legal_section", "section", "ipc section"},
    "case_count": {"case count", "case_count", "count", "cases", "total cases", "number of cases"},
    "geographic_level": {"geographic level", "geographic_level", "level", "geography"},
    "source_publication_date": {"source publication date", "source_publication_date", "publication date"},
    "notes": {"notes", "remarks", "note"},
}
VALID_LEVELS = {"district", "police_station", "state", "national"}


def normalize_name(value: str) -> str:
    value = unicodedata.normalize("NFKD", value or "")
    value = "".join(char for char in value if not unicodedata.combining(char))
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9]+", " ", value.lower())).strip()


def _headers_and_rows(raw: bytes, filename: str) -> tuple[list[str], list[dict[str, Any]]]:
    lower = filename.lower()
    if lower.endswith(".csv"):
        decoded = raw.decode("utf-8-sig")
        reader = csv.DictReader(io.StringIO(decoded))
        return list(reader.fieldnames or []), list(reader)
    if lower.endswith((".xlsx", ".xls")):
        frame = pd.read_excel(io.BytesIO(raw), dtype=object)
        frame = frame.where(pd.notnull(frame), None)
        return [str(column) for column in frame.columns], frame.to_dict(orient="records")
    raise ValueError("Only CSV, XLSX, and XLS files are supported")


def _map_headers(headers: list[str]) -> dict[str, str]:
    normalized = {normalize_name(header): header for header in headers}
    mapping: dict[str, str] = {}
    for internal, aliases in COLUMN_ALIASES.items():
        for alias in aliases:
            source = normalized.get(normalize_name(alias))
            if source:
                mapping[internal] = source
                break
    return mapping


def _string(row: dict[str, Any], mapping: dict[str, str], field: str) -> str | None:
    source = mapping.get(field)
    value = row.get(source) if source else None
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _as_int(value: str | None) -> int | None:
    if value is None:
        return None
    try:
        numeric = float(value.replace(",", ""))
        return int(numeric) if numeric.is_integer() else None
    except ValueError:
        return None


def _as_date(value: str | None) -> date | None:
    if not value:
        return None
    try:
        return date.fromisoformat(value)
    except ValueError:
        return None


async def import_crime_file(
    db: AsyncSession,
    *, raw: bytes,
    filename: str,
    source_name: str,
    source_url: str,
    replace_source: bool = False,
) -> dict[str, Any]:
    headers, rows = _headers_and_rows(raw, filename)
    mapping = _map_headers(headers)
    missing = sorted(REQUIRED_INTERNAL_COLUMNS - set(mapping))
    preview = [{str(k): (None if v is None else str(v)) for k, v in row.items()} for row in rows[:5]]
    if missing:
        report = {
            "import_id": "", "filename": filename, "status": "rejected", "total_rows": len(rows),
            "accepted_rows": 0, "rejected_rows": len(rows), "duplicate_rows": 0,
            "headers": headers, "preview": preview, "missing_columns": missing,
            "errors": [f"Required columns missing: {', '.join(missing)}"],
        }
        return report

    accepted: list[dict[str, Any]] = []
    errors: list[str] = []
    duplicate_rows = 0
    seen: set[tuple[Any, ...]] = set()
    for row_number, row in enumerate(rows, start=2):
        year = _as_int(_string(row, mapping, "reporting_year"))
        count = _as_int(_string(row, mapping, "case_count"))
        state = _string(row, mapping, "state")
        district = _string(row, mapping, "district_as_reported")
        category = _string(row, mapping, "crime_category")
        station = _string(row, mapping, "police_station")
        source_publication_date = _as_date(_string(row, mapping, "source_publication_date"))
        level = (_string(row, mapping, "geographic_level") or ("police_station" if station else "district")).lower()
        if not year or year < 1900 or year > date.today().year:
            errors.append(f"Row {row_number}: reporting_year must be between 1900 and {date.today().year}.")
            continue
        if count is None or count < 0:
            errors.append(f"Row {row_number}: case_count must be a non-negative whole number.")
            continue
        if not all((state, district, category)):
            errors.append(f"Row {row_number}: state, district, and crime category are required.")
            continue
        if level not in VALID_LEVELS:
            errors.append(f"Row {row_number}: geographic_level must be one of {', '.join(sorted(VALID_LEVELS))}.")
            continue
        if level == "police_station" and not station:
            errors.append(f"Row {row_number}: police_station is required for police_station geography.")
            continue
        if mapping.get("source_publication_date") and source_publication_date is None:
            errors.append(f"Row {row_number}: source_publication_date must use YYYY-MM-DD.")
            continue
        item = {
            "reporting_year": year, "reporting_period": _string(row, mapping, "reporting_period"),
            "state": state, "district_as_reported": district, "normalized_district": normalize_name(district),
            "police_station": station, "crime_category": category, "legal_section": _string(row, mapping, "legal_section"),
            "case_count": count, "geographic_level": level, "notes": _string(row, mapping, "notes"),
            "source_publication_date": source_publication_date,
        }
        key = (year, state.casefold(), item["normalized_district"], (station or "").casefold(), category.casefold(), level)
        if key in seen:
            duplicate_rows += 1
            errors.append(f"Row {row_number}: duplicate within upload.")
            continue
        seen.add(key)
        accepted.append(item)

    # Never delete an existing source before the replacement has at least one validated row.
    existing_rows = [] if replace_source else (await db.execute(
        select(CrimeStatistic).where(CrimeStatistic.source_name == source_name)
    )).scalars().all()
    existing_keys = {
        (r.reporting_year, r.state.casefold(), r.normalized_district, (r.police_station or "").casefold(),
         r.crime_category.casefold(), r.geographic_level) for r in existing_rows
    }
    valid = []
    for item in accepted:
        key = (item["reporting_year"], item["state"].casefold(), item["normalized_district"],
               (item["police_station"] or "").casefold(), item["crime_category"].casefold(), item["geographic_level"])
        if key in existing_keys:
            duplicate_rows += 1
            continue
        valid.append(item)

    if replace_source and valid:
        await db.execute(delete(CrimeStatistic).where(CrimeStatistic.source_name == source_name))

    data_import = DataImport(
        filename=filename, source_name=source_name.strip(), source_url=source_url.strip(), total_rows=len(rows),
        accepted_rows=len(valid), rejected_rows=len(rows) - len(valid),
        status="completed" if valid else "rejected",
        validation_report=json.dumps({"errors": errors[:100], "duplicates": duplicate_rows, "header_mapping": mapping}),
    )
    db.add(data_import)
    await db.flush()
    for item in valid:
        db.add(CrimeStatistic(**item, source_name=source_name.strip(), source_url=source_url.strip(), data_import_id=data_import.id))
    await db.commit()
    return {
        "import_id": data_import.id, "filename": filename, "status": data_import.status,
        "total_rows": len(rows), "accepted_rows": len(valid), "rejected_rows": len(rows) - len(valid),
        "duplicate_rows": duplicate_rows, "headers": headers, "preview": preview,
        "missing_columns": [], "errors": errors[:100],
    }
