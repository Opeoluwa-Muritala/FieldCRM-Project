from pathlib import Path
from functools import lru_cache

SQL_BASE = Path(__file__).parent.parent / "domains"

@lru_cache(maxsize=256)
def load_sql(domain: str, query_name: str) -> str:
    path = SQL_BASE / domain / "queries" / f"{query_name}.sql"
    if not path.exists():
        raise FileNotFoundError(f"SQL query not found: {path}")
    return path.read_text(encoding="utf-8").strip()
