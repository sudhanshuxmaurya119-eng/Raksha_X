-- New tables only: existing deployments also create them at startup through Base.metadata.create_all.
-- Run this SQL through the production database migration process before deploying where DDL is managed externally.
CREATE TABLE IF NOT EXISTS data_imports (
    id VARCHAR PRIMARY KEY, filename VARCHAR(255) NOT NULL, source_name VARCHAR(200) NOT NULL,
    source_url TEXT NOT NULL, imported_at TIMESTAMP NOT NULL, total_rows INTEGER NOT NULL,
    accepted_rows INTEGER NOT NULL, rejected_rows INTEGER NOT NULL, status VARCHAR(32) NOT NULL,
    validation_report TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS crime_statistics (
    id VARCHAR PRIMARY KEY, data_import_id VARCHAR REFERENCES data_imports(id), reporting_year INTEGER NOT NULL,
    reporting_period VARCHAR(80), state VARCHAR(120) NOT NULL, district_as_reported VARCHAR(160) NOT NULL,
    normalized_district VARCHAR(160) NOT NULL, police_station VARCHAR(160), crime_category VARCHAR(200) NOT NULL,
    legal_section VARCHAR(160), case_count INTEGER NOT NULL, geographic_level VARCHAR(32) NOT NULL,
    source_name VARCHAR(200) NOT NULL, source_url TEXT NOT NULL, source_publication_date DATE,
    imported_at TIMESTAMP NOT NULL, data_quality_status VARCHAR(32) NOT NULL, notes TEXT,
    CONSTRAINT uq_crime_statistic_source_row UNIQUE (reporting_year, state, normalized_district, police_station, crime_category, geographic_level, source_name)
);
CREATE TABLE IF NOT EXISTS police_stations (
    id VARCHAR PRIMARY KEY, official_name VARCHAR(160) NOT NULL UNIQUE, normalized_name VARCHAR(160) NOT NULL,
    address TEXT, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, public_phone VARCHAR(50), source_url TEXT NOT NULL,
    verification_status VARCHAR(32) NOT NULL, last_verified_at TIMESTAMP, operating_hours VARCHAR(160)
);
CREATE TABLE IF NOT EXISTS support_services (
    id VARCHAR PRIMARY KEY, name VARCHAR(200) NOT NULL, service_type VARCHAR(80) NOT NULL, description TEXT,
    address TEXT NOT NULL, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, public_phone VARCHAR(50),
    official_website TEXT, services_offered TEXT NOT NULL, operating_hours VARCHAR(160), eligibility_notes TEXT,
    verification_status VARCHAR(32) NOT NULL, verification_source TEXT NOT NULL, last_verified_at TIMESTAMP
);
