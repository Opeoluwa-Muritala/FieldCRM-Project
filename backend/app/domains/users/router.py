from fastapi import APIRouter, Depends
from app.core.database import db_conn
from app.domains.users.repository import UserRepository
from app.domains.users.service import UserService
from app.domains.users.schemas import UserResponse, UserCreate, UserRow
from app.core.dependencies import get_current_user, RoleChecker

router = APIRouter()

def get_user_service(conn = Depends(db_conn)) -> UserService:
    repo = UserRepository(conn)
    return UserService(repo)

@router.post("/register-org", response_model=UserResponse)
async def register_organisation(
    org_name: str,
    org_type: str,
    admin_name: str,
    admin_email: str,
    admin_password: str,
    service: UserService = Depends(get_user_service)
):
    user = await service.register_organisation(
        org_name=org_name,
        org_type=org_type,
        admin_name=admin_name,
        admin_email=admin_email,
        admin_password=admin_password
    )
    return UserResponse(
        id=str(user.id),
        org_id=str(user.org_id),
        full_name=user.full_name,
        email=user.email,
        role=user.role,
        active=user.active,
        created_at=user.created_at
    )

@router.post("/register-user", response_model=UserResponse)
async def register_user(
    user_in: UserCreate,
    service: UserService = Depends(get_user_service),
    current_admin: UserRow = Depends(RoleChecker(["System Admin", "Branch Manager"]))
):
    user = await service.register_user(current_admin, user_in)
    return UserResponse(
        id=str(user.id),
        org_id=str(user.org_id),
        full_name=user.full_name,
        email=user.email,
        role=user.role,
        active=user.active,
        created_at=user.created_at
    )
