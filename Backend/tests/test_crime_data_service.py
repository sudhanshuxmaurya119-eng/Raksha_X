import unittest
from pathlib import Path

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from database import Base
from models.crime_statistic import CrimeStatistic
from services.crime_data_service import import_crime_file, normalize_name


class CrimeDataImportTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with self.engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)
        self.sessions = async_sessionmaker(self.engine, expire_on_commit=False)

    async def asyncTearDown(self):
        await self.engine.dispose()

    async def test_csv_import_and_district_normalization(self):
        csv = b"Year,State,District,Category,Count\n2023,Uttar Pradesh,Meerut,Assault,12\n"
        async with self.sessions() as session:
            report = await import_crime_file(
                session, raw=csv, filename="ncrb.csv", source_name="NCRB test",
                source_url="https://example.gov.in/source",
            )
            self.assertEqual(report["accepted_rows"], 1)
            row = await session.scalar(select(CrimeStatistic))
            self.assertEqual(row.normalized_district, "meerut")
            self.assertEqual(row.case_count, 12)

    async def test_invalid_and_duplicate_rows_are_not_stored(self):
        csv = (
            b"Year,State,District,Category,Count\n"
            b"2023,Uttar Pradesh,Meerut,Assault,-2\n"
            b"2023,Uttar Pradesh,Meerut,Assault,4\n"
            b"2023,Uttar Pradesh,Meerut,Assault,4\n"
        )
        async with self.sessions() as session:
            report = await import_crime_file(
                session, raw=csv, filename="bad.csv", source_name="NCRB test",
                source_url="https://example.gov.in/source",
            )
            self.assertEqual(report["accepted_rows"], 1)
            self.assertEqual(report["duplicate_rows"], 1)
            self.assertEqual(await session.scalar(select(func.count()).select_from(CrimeStatistic)), 1)

    async def test_missing_required_columns_is_rejected_without_writing(self):
        async with self.sessions() as session:
            report = await import_crime_file(
                session, raw=b"Year,District,Count\n2023,Meerut,3\n", filename="incomplete.csv",
                source_name="NCRB test", source_url="https://example.gov.in/source",
            )
            self.assertEqual(report["status"], "rejected")
            self.assertIn("state", report["missing_columns"])
            self.assertEqual(await session.scalar(select(func.count()).select_from(CrimeStatistic)), 0)

    async def test_attached_ncrb_table_transcription_imports_with_expected_total(self):
        source_file = Path(__file__).parents[1] / "data" / "ncrb_up_crimes_against_women_2015.csv"
        async with self.sessions() as session:
            report = await import_crime_file(
                session, raw=source_file.read_bytes(), filename=source_file.name,
                source_name="NCRB Table 5.1 - Uttar Pradesh 2015",
                source_url="https://www.data.gov.in/catalog/district-wise-crimes-committed-against-women",
            )
            self.assertEqual(report["accepted_rows"], 76)
            self.assertEqual(await session.scalar(select(func.sum(CrimeStatistic.case_count))), 35527)
            meerut = await session.scalar(select(CrimeStatistic).where(CrimeStatistic.normalized_district == "meerut"))
            self.assertEqual(meerut.case_count, 859)


class DistrictNormalizationTests(unittest.TestCase):
    def test_normalizes_spacing_and_punctuation_without_mutating_source_value(self):
        self.assertEqual(normalize_name("Meerut  (District)"), "meerut district")
