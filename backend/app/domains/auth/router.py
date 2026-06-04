from fastapi import APIRouter, Depends, Response
from fastapi.security import OAuth2PasswordRequestForm
from app.core.database import db_conn
from app.domains.auth.repository import AuthRepository
from app.domains.auth.service import AuthService
from app.domains.auth.schemas import Token

router = APIRouter()

def get_auth_service(conn = Depends(db_conn)) -> AuthService:
    repo = AuthRepository(conn)
    return AuthService(repo)

@router.post("/login", response_model=Token)
async def login_cookie(
    response: Response,
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service)
):
    token = await service.authenticate_user(form_data.username, form_data.password)
    
    # Configure HttpOnly cookie for session tracking
    response.set_cookie(
        key="session",
        value=token,
        httponly=True,
        secure=False,
        samesite="strict",
        max_age=30 * 60,
        path="/"
    )
    return {"access_token": token, "token_type": "bearer"}

@router.post("/login-bearer", response_model=Token)
async def login_bearer(
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service)
):
    token = await service.authenticate_user(form_data.username, form_data.password)
    return {"access_token": token, "token_type": "bearer"}

@router.post("/logout")
def logout(response: Response):
    response.delete_cookie(key="session", path="/")
    response.delete_cookie(key="__Host-session", path="/")
    return {"status": "logged_out"}
