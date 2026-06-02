from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.db.models import Borrower
from app.core import security
from app.core.config import settings
from app.schemas import portal as schemas

router = APIRouter()

@router.post("/login")
def portal_login(
    response: Response,
    login_in: schemas.PortalLoginRequest,
    db: Session = Depends(get_db)
):
    """
    Verifies borrower credentials and registers a secure HttpOnly customer session cookie:
    - Blocks brute-force XSS token hijacking by isolating token access completely.
    """
    borrower = db.query(Borrower).filter(
        Borrower.phone == login_in.phone,
        Borrower.bvn == login_in.bvn
    ).first()
    
    if not borrower:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials. Verify Phone and BVN details align with active records."
        )
        
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    token = security.create_access_token(
        borrower.id, expires_delta=access_token_expires
    )
    
    response.set_cookie(
        key="borrower_session",
        value=token,
        httponly=True,
        secure=False,  # Enforce True in Production
        samesite="strict",
        max_age=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        path="/"
    )
    return {"status": "success"}

@router.post("/logout")
def portal_logout(response: Response):
    """Clears client session cookie to terminate access."""
    response.delete_cookie(key="borrower_session", path="/")
    return {"status": "logged_out"}
