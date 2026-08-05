import secrets
from datetime import datetime, timedelta, timezone
from uuid import UUID

from app.domains.users.repository import UserRepository
from app.domains.auth.repository import AuthRepository
from app.core.security import get_password_hash
from app.core.exceptions import DomainException
from app.domains.users.schemas import UserRow
from app.core.database import get_transaction

class UserService:
    ALLOWED_ROLES = {
        "account_officer", "branch_manager", "branch_supervisor", "credit_analyst",
        "crm", "head_crm", "auditor", "ed", "md", "legal", "system_admin",
        "team_lead", "relationship_officer", "supervisor",
    }
    def __init__(self, repo: UserRepository):
        self.repo = repo

    async def register_organisation(
        self,
        org_name: str,
        org_type: str,
        admin_name: str,
        admin_email: str,
        admin_password: str
    ) -> UserRow:
        existing = await self.repo.get_by_email(admin_email)
        if existing:
            raise DomainException("A user with this email already exists.", 400)

        # Use transaction for multi-step registration
        async with get_transaction() as conn:
            tx_repo = UserRepository(conn)
            org_code = org_name.lower().replace(" ", "_")
            org = await tx_repo.create_organisation(org_name, org_code)
            
            hashed = get_password_hash(admin_password)
            user = await tx_repo.create_user(
                org_id=org.id,
                full_name=admin_name,
                email=admin_email,
                role="system_admin",
                password_hash=hashed
            )
            return user

    async def register_user(self, current_admin: UserRow, user_in) -> UserRow:
        if str(current_admin.org_id) != str(user_in.org_id):
            raise DomainException("Cannot register user outside your own organisation.", 403)

        existing = await self.repo.get_by_email(user_in.email)
        if existing:
            raise DomainException("A user with this email already exists.", 400)

        hashed = get_password_hash(user_in.password)
        db_role = user_in.role.lower().replace(" ", "_")
        if db_role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)
        
        # Equate Team Lead -> branch_manager, Relationship Officer -> account_officer, Supervisor -> branch_supervisor
        role_map = {
            "team_lead": "branch_manager",
            "relationship_officer": "account_officer",
            "supervisor": "branch_supervisor",
        }
        db_role = role_map.get(db_role, db_role)

        user = await self.repo.create_user(
            org_id=user_in.org_id,
            full_name=user_in.full_name,
            email=user_in.email,
            role=db_role,
            password_hash=hashed,
            branch_id=user_in.branch_id
        )
        return user

    async def invite_user(self, current_admin: UserRow, invite_in) -> tuple[UserRow, str]:
        email = str(invite_in.email).strip().lower()
        if await self.repo.get_by_email(email):
            raise DomainException("A user with this email already exists.", 400)

        role = invite_in.role.strip().lower().replace(" ", "_")
        if role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)

        # Equate Team Lead -> branch_manager, Relationship Officer -> account_officer, Supervisor -> branch_supervisor
        role_map = {
            "team_lead": "branch_manager",
            "relationship_officer": "account_officer",
            "supervisor": "branch_supervisor",
        }
        role = role_map.get(role, role)

        user = await self.repo.create_user(
            org_id=current_admin.org_id,
            full_name=invite_in.full_name.strip(),
            email=email,
            role=role,
            password_hash=get_password_hash(secrets.token_urlsafe(32)),
            branch_id=invite_in.branch_id
        )
        await self.repo.deactivate_user(user.id)
        token = secrets.token_urlsafe(32)
        await AuthRepository(self.repo.conn).create_reset_token(
            str(user.id), token, datetime.now(timezone.utc) + timedelta(hours=72)
        )
        user.active = False
        return user, token

    async def update_user_role(self, current_admin: UserRow, user_id, role: str) -> UserRow:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot change your own role.", 400)

        normalized_role = role.strip().lower().replace(" ", "_")
        if normalized_role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)

        # Equate Team Lead -> branch_manager, Relationship Officer -> account_officer, Supervisor -> branch_supervisor
        role_map = {
            "team_lead": "branch_manager",
            "relationship_officer": "account_officer",
            "supervisor": "branch_supervisor",
        }
        normalized_role = role_map.get(normalized_role, normalized_role)

        await self.repo.update_role(user.id, normalized_role)
        return await self.repo.get_by_id(user.id)

    async def deactivate_managed_user(self, current_admin: UserRow, user_id) -> None:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot deactivate your own account.", 400)
        if not user.active:
            raise DomainException("This user is already inactive.", 400)

        await self.repo.deactivate_user(user.id)

    async def delete_managed_user(self, current_admin: UserRow, user_id) -> None:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot delete your own account.", 400)
        await self.repo.delete_user(user.id)

    async def update_user_branch(self, current_admin: UserRow, user_id, branch_id: UUID | None) -> UserRow:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot change your own branch.", 400)

        await self.repo.update_branch(user.id, branch_id)
        return await self.repo.get_by_id(user.id)
