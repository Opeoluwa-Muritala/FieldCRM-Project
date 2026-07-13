from fastapi import APIRouter, Depends, Header, HTTPException, Request, status
from app.core.database import db_conn
from app.domains.users.repository import UserRepository
from app.domains.users.service import UserService
from app.domains.users.schemas import UserResponse, UserCreate, UserInvitationCreate, UserRoleUpdate, UserRow
from uuid import UUID
from app.core.dependencies import get_current_user, RoleChecker
from app.core.config import settings
from app.services.email_service import EmailService

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
    x_registration_secret: str = Header(default=""),
    service: UserService = Depends(get_user_service)
):
    required = settings.ORG_REGISTRATION_SECRET
    if not required or x_registration_secret != required:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid or missing registration secret.",
        )
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


@router.post("/invitations", status_code=status.HTTP_201_CREATED)
async def invite_user(
    request: Request,
    invitation: UserInvitationCreate,
    service: UserService = Depends(get_user_service),
    current_admin: UserRow = Depends(RoleChecker(["System Admin"])),
):
    user, token = await service.invite_user(current_admin, invitation)
    base_url = settings.APP_BASE_URL.rstrip("/") or str(request.base_url).rstrip("/")
    delivered = EmailService().send_invitation(
        recipient=user.email,
        full_name=user.full_name,
        role=user.role,
        invitation_url=f"{base_url}/accept-invitation?token={token}",
    )
    return {
        "id": str(user.id), "email": user.email, "role": user.role, "email_sent": delivered,
        "message": "Invitation email sent." if delivered else "User was invited, but the email could not be delivered. Check SMTP settings and server logs before resending.",
    }


@router.put("/{user_id}/role")
async def update_user_role(
    user_id: UUID,
    update: UserRoleUpdate,
    service: UserService = Depends(get_user_service),
    current_admin: UserRow = Depends(RoleChecker(["System Admin"])),
):
    user = await service.update_user_role(current_admin, user_id, update.role)
    return {"id": str(user.id), "role": user.role, "message": "Role updated."}


@router.post("/{user_id}/deactivate")
async def deactivate_user(
    user_id: UUID,
    service: UserService = Depends(get_user_service),
    current_admin: UserRow = Depends(RoleChecker(["System Admin"])),
):
    await service.deactivate_managed_user(current_admin, user_id)
    return {"id": str(user_id), "active": False, "message": "User deactivated."}
