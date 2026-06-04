from app.core.sql import load_sql

class BaseRepository:
    domain: str  # set by subclass

    def __init__(self, conn):
        self.conn = conn

    def sql(self, query_name: str) -> str:
        return load_sql(self.domain, query_name)
