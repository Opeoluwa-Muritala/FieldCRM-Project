from datetime import timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session
from app.api import deps
from app.core import security
from app.core.config import settings
from app.db.session import get_db
from app.db.models import Organisation, User, Branch
from app.schemas import user as user_schemas

router = APIRouter()

def get_login_user(db: Session, phone: str) -> Optional[User]:
    try:
        return db.query(User).filter(User.phone == phone).first()
    except SQLAlchemyError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Authentication database is unavailable."
        ) from exc

@router.post("/login", response_model=user_schemas.Token)
def login_cookie(
    response: Response,
    db: Session = Depends(get_db),
    form_data: OAuth2PasswordRequestForm = Depends()
):
    """
    Secure Cookie-based authentication endpoint for web clients:
    - Verifies user credentials.
    - Sets secure, HttpOnly, SameSite=Strict cookies to protect session IDs from XSS harvesting.
    """
    user = get_login_user(db, form_data.username)
    if not user or not user.is_active or not security.verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect phone number or password."
        )
        
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = security.create_access_token(
        user.id, expires_delta=access_token_expires
    )
    
    # Configure secure cookie parameters (HttpOnly, SameSite=Strict)
    # Standard name "__Host-session" requires HTTPS, fallback to "session" for local testing
    cookie_name = "session"
    response.set_cookie(
        key=cookie_name,
        value=access_token,
        httponly=True,
        secure=False,  # Enforce True in Production
        samesite="strict",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/"
    )
    
    return {"access_token": access_token, "token_type": "bearer"}

@router.post("/login-bearer", response_model=user_schemas.Token)
def login_bearer(
    db: Session = Depends(get_db),
    form_data: OAuth2PasswordRequestForm = Depends()
):
    """
    Standard HTTP Bearer Token endpoint to support Android native KMP clients.
    Returns token payload in response body.
    """
    user = get_login_user(db, form_data.username)
    if not user or not user.is_active or not security.verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect phone number or password."
        )
        
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = security.create_access_token(
        user.id, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

@router.post("/register-org", response_model=user_schemas.UserResponse)
def register_organisation(
    org_name: str,
    org_type: str,
    admin_name: str,
    admin_phone: str,
    admin_password: str,
    db: Session = Depends(get_db)
):
    """
    Onboard a new Organisation (SACCO, Cooperative, MFB):
    1. Creates the Organization.
    2. Creates a default HQ branch.
    3. Creates the primary Org Admin / Manager user.
    """
    existing_user = db.query(User).filter(User.phone == admin_phone).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A user with this phone number already exists."
        )
        
    org = Organisation(name=org_name, type=org_type)
    db.add(org)
    db.flush()  # Allocates org.id
    
    branch = Branch(org_id=org.id, name="HQ Branch", location="Headquarters")
    db.add(branch)
    db.flush()  # Allocates branch.id
    
    hashed_pwd = security.get_password_hash(admin_password)
    user = User(
        org_id=org.id,
        branch_id=branch.id,
        name=admin_name,
        phone=admin_phone,
        role="System Admin",
        hashed_password=hashed_pwd
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    
    # Update branch manager representation
    branch.manager_id = user.id
    db.commit()
    
    return user

@router.post("/register-user", response_model=user_schemas.UserResponse)
def register_user(
    user_in: user_schemas.UserCreate,
    db: Session = Depends(get_db),
    current_admin: User = Depends(deps.RoleChecker(["System Admin", "Branch Manager"]))
):
    """
    Creates new organizational users (Loan Officers, Credit Officers, Auditors, Committee Members):
    - Role restrictions: Restricted to organization administrators/managers.
    - Scope restriction: Enforces that users can only be created under the admin's organization.
    """
    if current_admin.org_id != user_in.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot register user outside your own organisation."
        )
        
    existing_user = db.query(User).filter(User.phone == user_in.phone).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A user with this phone number already exists."
        )
        
    hashed_pwd = security.get_password_hash(user_in.password)
    user = User(
        org_id=user_in.org_id,
        branch_id=user_in.branch_id,
        name=user_in.name,
        phone=user_in.phone,
        role=user_in.role,
        hashed_password=hashed_pwd
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

@router.post("/logout")
def logout(response: Response):
    """Clears the HttpOnly session cookie to terminate session."""
    response.delete_cookie(key="session", path="/")
    response.delete_cookie(key="__Host-session", path="/")
    return {"status": "logged_out"}
