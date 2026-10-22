import unittest
from datetime import datetime

from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from database import Base, get_db
from auth_dependencies import require_safety_data_admin
from config import settings
from models.crime_statistic import CrimeStatistic
from models.user import User
from routers.crime_heatmap import router


class HistoricalSafetyApiTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with self.engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)
        self.sessions = async_sessionmaker(self.engine, expire_on_commit=False)
        async with self.sessions() as session:
            session.add(CrimeStatistic(
                reporting_year=2023, state="Uttar Pradesh", district_as_reported="Meerut",
                normalized_district="meerut", crime_category="Assault", case_count=12,
                geographic_level="district", source_name="NCRB", source_url="https://example.gov.in", imported_at=datetime.utcnow(),
            ))
            await session.commit()
        self.app = FastAPI()
        self.app.include_router(router, prefix="/safety")

        async def override_db():
            async with self.sessions() as session:
                yield session

        self.app.dependency_overrides[get_db] = override_db
        self.client = TestClient(self.app)

    async def asyncTearDown(self):
        self.client.close()
        await self.engine.dispose()

    async def test_overview_and_empty_geometry_are_privacy_safe(self):
        overview = self.client.get("/safety/overview?year=2023")
        self.assertEqual(overview.status_code, 200)
        self.assertTrue(overview.json()["has_historical_data"])
        self.assertEqual(overview.json()["historical_case_count"], 12)

        heatmap = self.client.get("/safety/heatmap?year=2023")
        self.assertEqual(heatmap.status_code, 200)
        self.assertEqual(heatmap.json()["type"], "FeatureCollection")
        self.assertEqual(heatmap.json()["features"], [])

    async def test_support_endpoint_excludes_unverified_or_unlocated_records(self):
        response = self.client.get("/safety/support-services")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

    async def test_import_authorization_uses_server_side_allowlist(self):
        previous = settings.SAFETY_DATA_ADMIN_EMAILS
        settings.SAFETY_DATA_ADMIN_EMAILS = "admin@example.org"
        try:
            allowed = User(username="admin", email="admin@example.org", password_hash="not-used")
            self.assertIs(await require_safety_data_admin(allowed), allowed)
            denied = User(username="member", email="member@example.org", password_hash="not-used")
            with self.assertRaises(Exception) as error:
                await require_safety_data_admin(denied)
            self.assertEqual(error.exception.status_code, 403)
        finally:
            settings.SAFETY_DATA_ADMIN_EMAILS = previous
