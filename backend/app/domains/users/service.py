import secrets
from datetime import datetime, timedelta, timezone
from uuid import UUID

from app.domains.users.repository import UserRepository
from app.domains.auth.repository import AuthRepository
from app.core.security import get_password_hash
from app.core.exceptions import DomainException
from app.domains.users.schemas import UserRow
from app.core.database import get_transaction
from app.core.cache import invalidate_auth_user
from app.core.loan_authorization import canonical_role

class UserService:
    ALLOWED_ROLES = {
        "account_officer", "branch_manager", "branch_supervisor", "credit_analyst",
        "crm", "head_crm", "auditor", "ed", "md", "legal", "system_admin",
    }
    def __init__(self, repo: UserRepository):
        self.repo = repo

    async def _validate_branch(self, org_id: UUID, branch_id: UUID | None) -> None:
        if branch_id is None:
            return
        from app.domains.branches.repository import BranchRepository
        if not await BranchRepository(self.repo.conn).belongs_to_org(branch_id, org_id):
            raise DomainException("Select a valid branch.", 400)

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
            
            try:
                hashed = get_password_hash(admin_password)
            except ValueError as exc:
                raise DomainException(str(exc), 400) from exc
            user = await tx_repo.create_user(
                org_id=org.id,
                full_name=admin_name,
                email=admin_email,
                role="system_admin",
                password_hash=hashed
            )
            return user

    async def register_user(self, current_admin: UserRow, user_in) -> UserRow:
        if canonical_role(current_admin.role) != "system_admin":
            raise DomainException("Only a system administrator can create users.", 403)
        if str(current_admin.org_id) != str(user_in.org_id):
            raise DomainException("Cannot register user outside your own organisation.", 403)

        existing = await self.repo.get_by_email(user_in.email)
        if existing:
            raise DomainException("A user with this email already exists.", 400)

        try:
            hashed = get_password_hash(user_in.password)
        except ValueError as exc:
            raise DomainException(str(exc), 400) from exc
        db_role = canonical_role(user_in.role)
        if db_role == "configuration_admin":
            raise DomainException("Configuration Admins must be provisioned through the restricted control process.", 403)
        if db_role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)
        await self._validate_branch(current_admin.org_id, user_in.branch_id)

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

        role = canonical_role(invite_in.role)
        if role == "configuration_admin":
            raise DomainException("Configuration Admins must be provisioned through the restricted control process.", 403)
        if role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)
        await self._validate_branch(current_admin.org_id, invite_in.branch_id)

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

        normalized_role = canonical_role(role)
        if normalized_role == "configuration_admin" or canonical_role(user.role) == "configuration_admin":
            raise DomainException("System Admin cannot grant or change Configuration Admin access.", 403)
        if normalized_role not in self.ALLOWED_ROLES:
            raise DomainException("Select a valid role.", 400)

        await self.repo.update_role(user.id, normalized_role)
        await invalidate_auth_user(user.id)
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
        await invalidate_auth_user(user.id)

    async def delete_managed_user(self, current_admin: UserRow, user_id) -> None:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot delete your own account.", 400)
        await self.repo.delete_user(user.id)
        await invalidate_auth_user(user.id)

    async def update_user_branch(self, current_admin: UserRow, user_id, branch_id: UUID | None) -> UserRow:
        user = await self.repo.get_by_id(user_id)
        if not user or user.org_id != current_admin.org_id:
            raise DomainException("User not found.", 404)
        if user.id == current_admin.id:
            raise DomainException("You cannot change your own branch.", 400)

        await self._validate_branch(current_admin.org_id, branch_id)
        await self.repo.update_branch(user.id, branch_id)
        await invalidate_auth_user(user.id)
        return await self.repo.get_by_id(user.id)
