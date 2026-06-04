from app.core.security import verify_password, create_access_token
from app.domains.auth.repository import AuthRepository
from app.core.exceptions import DomainException


class AuthService:
    def __init__(self, repo: AuthRepository):
        self.repo = repo

    async def authenticate_user(self, email: str, password: str) -> str:
        """Authenticate a user by email and password, return JWT token."""
        user = await self.repo.get_user_by_email(email)
        if not user or not user.is_active:
            raise DomainException("Incorrect email or password.", 401)

        if not verify_password(password, user.hashed_password):
            raise DomainException("Incorrect email or password.", 401)

        token = create_access_token(user.id, role=user.role, org_id=user.org_id)
        return token
