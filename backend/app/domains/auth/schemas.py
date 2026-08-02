from pydantic import BaseModel

class Token(BaseModel):
    access_token: str
    token_type: str
    refresh_token: str | None = None
    access_expires_in: int | None = None
    session_expires_at: str | None = None

class RefreshRequest(BaseModel):
    refresh_token: str

class LogoutRequest(BaseModel):
    refresh_token: str | None = None

class LoginRequest(BaseModel):
    username: str
    password: str
