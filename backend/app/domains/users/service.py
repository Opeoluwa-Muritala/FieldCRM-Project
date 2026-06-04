from app.domains.users.repository import UserRepository
from app.core.security import get_password_hash
from app.core.exceptions import DomainException
from app.domains.users.schemas import UserRow
from app.core.database import get_transaction

class UserService:
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
        user = await self.repo.create_user(
            org_id=user_in.org_id,
            full_name=user_in.full_name,
            email=user_in.email,
            role=db_role,
            password_hash=hashed
        )
        return user
