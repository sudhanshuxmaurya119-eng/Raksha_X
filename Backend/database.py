import ssl

from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy import inspect, text
from sqlalchemy.orm import DeclarativeBase
from config import settings

database_url = settings.DATABASE_URL
if database_url.startswith("postgres://"):
    database_url = "postgresql+asyncpg://" + database_url.removeprefix("postgres://")
elif database_url.startswith("postgresql://"):
    database_url = "postgresql+asyncpg://" + database_url.removeprefix("postgresql://")

database_options = {}
if database_url.startswith("postgresql+asyncpg://"):
    # Supabase PostgreSQL uses SSL. Disabling the asyncpg statement cache also
    # keeps transaction-pooler connections compatible with PgBouncer.
    connect_args = {"statement_cache_size": 0}

    if settings.DATABASE_SSL_REQUIRED:
        # Some local or proxied environments terminate TLS with a cert chain that
        # is not trusted by the system CA store. Use a TLS context with hostname
        # verification disabled to keep local Supabase access working while still
        # encrypting traffic.
        ssl_context = ssl.create_default_context()
        ssl_context.check_hostname = False
        ssl_context.verify_mode = ssl.CERT_NONE
        connect_args["ssl"] = ssl_context
    else:
        connect_args["ssl"] = False

    database_options["connect_args"] = connect_args

engine = create_async_engine(database_url, echo=False, **database_options)
AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

async def get_db():
    async with AsyncSessionLocal() as session:
        yield session

async def init_db():
    from models import user, incident, sos_contact, sos_event, sos_acknowledgement, push_token, saved_route  # noqa - import models to register them
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

        def add_user_age_column(sync_conn):
            columns = {column["name"] for column in inspect(sync_conn).get_columns("users")}
            if "age" not in columns:
                sync_conn.execute(text("ALTER TABLE users ADD COLUMN age INTEGER"))

        await conn.run_sync(add_user_age_column)
