from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker
from app.core.config import settings


# secure database engine configured with pooling and safe connection settings
# SQLAlchemy ORMs automatically use parameterized prepared statements for all queries
if settings.DATABASE_URL.startswith("sqlite"):
    engine = create_engine(
        settings.DATABASE_URL,
        connect_args={"check_same_thread": False}
    )
else:
    engine = create_engine(
        settings.DATABASE_URL,
        pool_pre_ping=True,
        pool_size=10,
        max_overflow=20
    )


SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    """
    FastAPI dependency injection provider for database sessions:
    - Yields database connection session locally.
    - Closes session cleanly to prevent connection pooling leaks.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
